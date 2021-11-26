/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirModuleResolveState
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LowLevelFirApiFacadeForResolveOnAir
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.FileDiagnosticRetriever
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.FileStructureElementDiagnostics
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.SingleNonLocalDeclarationDiagnosticRetriever
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.ModuleFileCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.RawFirNonLocalDeclarationBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.declarationCanBeLazilyResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.FirIdeProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.firIdeProvider
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.psi.*
import java.util.concurrent.ConcurrentHashMap

internal sealed class FileStructureElement(val firFile: FirFile, protected val lockProvider: LockProvider<FirFile>) {
    abstract val psi: KtAnnotated
    abstract val mappings: KtToFirMapping
    abstract val diagnostics: FileStructureElementDiagnostics
}

internal class KtToFirMapping(firElement: FirElement, recorder: FirElementsRecorder) {

    private val mapping = FirElementsRecorder.recordElementsFrom(firElement, recorder)

    private val userTypeMapping = ConcurrentHashMap<KtUserType, FirElement>()
    fun getElement(ktElement: KtElement, state: FirModuleResolveState): FirElement? {

        mapping[ktElement]?.let { return it }

        val userType = when (ktElement) {
            is KtUserType -> ktElement
            is KtNameReferenceExpression -> ktElement as? KtUserType
            else -> null
        } ?: return null

        //This is for not inner KtUserType
        if (userType.parent is KtTypeReference) return null

        return userTypeMapping.getOrPut(userType) {
            val typeReference = KtPsiFactory(ktElement.project).createType(userType.text)
            LowLevelFirApiFacadeForResolveOnAir.onAirResolveTypeInPlace(ktElement, typeReference, state)
        }
    }

    fun getFirOfClosestParent(element: KtElement, state: FirModuleResolveState): FirElement? {
        var current: PsiElement? = element
        while (current != null && current !is KtFile) {
            if (current is KtElement) {
                getElement(current, state)?.let { return it }
            }
            current = current.parent
        }
        return null
    }
}

internal sealed class ReanalyzableStructureElement<KT : KtDeclaration, S : FirBasedSymbol<*>>(
    firFile: FirFile,
    val firSymbol: S,
    lockProvider: LockProvider<FirFile>,
) : FileStructureElement(firFile, lockProvider) {
    abstract override val psi: KtDeclaration
    abstract val timestamp: Long

    /**
     * Creates new declaration by [newKtDeclaration] which will serve as replacement of [firSymbol]
     * Also, modify [firFile] & replace old version of declaration with a new one
     */
    abstract fun reanalyze(
        newKtDeclaration: KT,
        cache: ModuleFileCache,
        firLazyDeclarationResolver: FirLazyDeclarationResolver,
        firIdeProvider: FirIdeProvider,
    ): ReanalyzableStructureElement<KT, S>

    fun isUpToDate(): Boolean = psi.getModificationStamp() == timestamp

    override val diagnostics = FileStructureElementDiagnostics(
        firFile,
        lockProvider,
        SingleNonLocalDeclarationDiagnosticRetriever(firSymbol.fir)
    )

    companion object {
        val recorder = FirElementsRecorder()
    }
}

internal class ReanalyzableFunctionStructureElement(
    firFile: FirFile,
    override val psi: KtNamedFunction,
    firSymbol: FirFunctionSymbol<*>,
    override val timestamp: Long,
    lockProvider: LockProvider<FirFile>,
) : ReanalyzableStructureElement<KtNamedFunction, FirFunctionSymbol<*>>(firFile, firSymbol, lockProvider) {
    override val mappings = KtToFirMapping(firSymbol.fir, recorder)

    override fun reanalyze(
        newKtDeclaration: KtNamedFunction,
        cache: ModuleFileCache,
        firLazyDeclarationResolver: FirLazyDeclarationResolver,
        firIdeProvider: FirIdeProvider,
    ): ReanalyzableFunctionStructureElement {
        val originalFunction = firSymbol.fir as FirSimpleFunction
        val designation = originalFunction.collectDesignation()

        val temporaryFunction = RawFirNonLocalDeclarationBuilder.buildWithFunctionSymbolRebind(
            session = originalFunction.moduleData.session,
            scopeProvider = originalFunction.moduleData.session.firIdeProvider.kotlinScopeProvider,
            designation = designation,
            rootNonLocalDeclaration = newKtDeclaration,
        ) as FirSimpleFunction

        return cache.firFileLockProvider.withWriteLock(firFile) {

            val upgradedPhase = minOf(originalFunction.resolvePhase, FirResolvePhase.DECLARATIONS)
            with(originalFunction) {
                replaceBody(temporaryFunction.body)
                replaceContractDescription(temporaryFunction.contractDescription)
                replaceResolvePhase(upgradedPhase)
            }
            designation.toSequence(includeTarget = true).forEach {
                it.replaceResolvePhase(minOf(it.resolvePhase, upgradedPhase))
            }

            val resolvedDeclaration = firLazyDeclarationResolver.lazyResolveDeclaration(
                firDeclarationToResolve = originalFunction,
                moduleFileCache = cache,
                scopeSession = ScopeSession(),
                toPhase = FirResolvePhase.BODY_RESOLVE,
                checkPCE = true,
            )
            check(resolvedDeclaration === originalFunction) {
                "Reanalysed declaration not expected to be updated"
            }

            ReanalyzableFunctionStructureElement(
                firFile,
                newKtDeclaration,
                originalFunction.symbol,
                newKtDeclaration.modificationStamp,
                lockProvider,
            )
        }
    }
}

internal class ReanalyzablePropertyStructureElement(
    firFile: FirFile,
    override val psi: KtProperty,
    firSymbol: FirPropertySymbol,
    override val timestamp: Long,
    lockProvider: LockProvider<FirFile>,
) : ReanalyzableStructureElement<KtProperty, FirPropertySymbol>(firFile, firSymbol, lockProvider) {
    override val mappings = KtToFirMapping(firSymbol.fir, recorder)

    override fun reanalyze(
        newKtDeclaration: KtProperty,
        cache: ModuleFileCache,
        firLazyDeclarationResolver: FirLazyDeclarationResolver,
        firIdeProvider: FirIdeProvider,
    ): ReanalyzablePropertyStructureElement {
        val originalProperty = firSymbol.fir
        val designation = originalProperty.collectDesignation()

        val temporaryProperty = RawFirNonLocalDeclarationBuilder.buildWithFunctionSymbolRebind(
            session = originalProperty.moduleData.session,
            scopeProvider = originalProperty.moduleData.session.firIdeProvider.kotlinScopeProvider,
            designation = designation,
            rootNonLocalDeclaration = newKtDeclaration,
        ) as FirProperty

        return cache.firFileLockProvider.withWriteLock(firFile) {

            val getterPhase = originalProperty.getter?.resolvePhase ?: originalProperty.resolvePhase
            val setterPhase = originalProperty.setter?.resolvePhase ?: originalProperty.resolvePhase
            val upgradedPhase = minOf(originalProperty.resolvePhase, getterPhase, setterPhase, FirResolvePhase.DECLARATIONS)

            with(originalProperty) {
                getter?.replaceBody(temporaryProperty.getter?.body)
                setter?.replaceBody(temporaryProperty.setter?.body)
                replaceInitializer(temporaryProperty.initializer)
                getter?.replaceResolvePhase(upgradedPhase)
                setter?.replaceResolvePhase(upgradedPhase)
                replaceResolvePhase(upgradedPhase)
                replaceBodyResolveState(FirPropertyBodyResolveState.NOTHING_RESOLVED)
            }

            val resolvedDeclaration = firLazyDeclarationResolver.lazyResolveDeclaration(
                firDeclarationToResolve = originalProperty,
                moduleFileCache = cache,
                scopeSession = ScopeSession(),
                toPhase = FirResolvePhase.BODY_RESOLVE,
                checkPCE = true,
            )
            check(resolvedDeclaration === originalProperty) {
                "Reanalysed declaration not expected to be updated"
            }

            ReanalyzablePropertyStructureElement(
                firFile,
                newKtDeclaration,
                originalProperty.symbol,
                newKtDeclaration.modificationStamp,
                lockProvider,
            )
        }
    }
}

internal class NonReanalyzableDeclarationStructureElement(
    firFile: FirFile,
    val fir: FirDeclaration,
    override val psi: KtDeclaration,
    lockProvider: LockProvider<FirFile>,
) : FileStructureElement(firFile, lockProvider) {
    override val mappings = KtToFirMapping(fir, recorder)

    override val diagnostics = FileStructureElementDiagnostics(firFile, lockProvider, SingleNonLocalDeclarationDiagnosticRetriever(fir))


    companion object {
        private val recorder = object : FirElementsRecorder() {
            override fun visitProperty(property: FirProperty, data: MutableMap<KtElement, FirElement>) {
                val psi = property.psi as? KtProperty ?: return super.visitProperty(property, data)
                if (!isReanalyzableContainer(psi) || !declarationCanBeLazilyResolved(psi)) {
                    super.visitProperty(property, data)
                }
            }

            override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: MutableMap<KtElement, FirElement>) {
                val psi = simpleFunction.psi as? KtNamedFunction ?: return super.visitSimpleFunction(simpleFunction, data)
                if (!isReanalyzableContainer(psi) || !declarationCanBeLazilyResolved(psi)) {
                    super.visitSimpleFunction(simpleFunction, data)
                }
            }
        }
    }
}


internal class RootStructureElement(
    firFile: FirFile,
    override val psi: KtFile,
    lockProvider: LockProvider<FirFile>,
) : FileStructureElement(firFile, lockProvider) {
    override val mappings = KtToFirMapping(firFile, recorder)

    override val diagnostics = FileStructureElementDiagnostics(firFile, lockProvider, FileDiagnosticRetriever)

    companion object {
        private val recorder = object : FirElementsRecorder() {
            override fun visitElement(element: FirElement, data: MutableMap<KtElement, FirElement>) {
                if (element !is FirDeclaration || element is FirFile) {
                    super.visitElement(element, data)
                }
            }
        }
    }
}
