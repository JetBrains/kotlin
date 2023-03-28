/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LowLevelFirApiFacadeForResolveOnAir
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.FileDiagnosticRetriever
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.FileStructureElementDiagnostics
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.SingleNonLocalDeclarationDiagnosticRetriever
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.RawFirNonLocalDeclarationBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.declarationCanBeLazilyResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.withInvalidationOnException
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.psi.*
import java.util.concurrent.ConcurrentHashMap

internal sealed class FileStructureElement(val firFile: FirFile, protected val moduleComponents: LLFirModuleResolveComponents) {
    abstract val psi: KtAnnotated
    abstract val mappings: KtToFirMapping
    abstract val diagnostics: FileStructureElementDiagnostics
}

internal class KtToFirMapping(firElement: FirElement, recorder: FirElementsRecorder) {
    private val mapping = FirElementsRecorder.recordElementsFrom(firElement, recorder)
    private val userTypeMapping = ConcurrentHashMap<KtUserType, FirElement>()

    fun getElement(ktElement: KtElement, firResolveSession: LLFirResolveSession): FirElement? {
        mapping[ktElement]?.let { return it }

        val userType = when (ktElement) {
            is KtUserType -> ktElement
            is KtNameReferenceExpression -> ktElement.parent as? KtUserType
            else -> null
        } ?: return null

        //This is for not inner KtUserType
        if (userType.parent is KtTypeReference) return null

        return userTypeMapping.getOrPut(userType) {
            val typeReference = KtPsiFactory(ktElement.project).createType(userType.text)
            LowLevelFirApiFacadeForResolveOnAir.onAirResolveTypeInPlace(ktElement, typeReference, firResolveSession)
        }
    }

    fun getFirOfClosestParent(element: KtElement, firResolveSession: LLFirResolveSession): FirElement? {
        var current: PsiElement? = element
        while (current != null && current !is KtFile) {
            if (current is KtElement) {
                getElement(current, firResolveSession)?.let { return it }
            }
            current = current.parent
        }
        return null
    }
}

internal sealed class ReanalyzableStructureElement<KT : KtDeclaration, S : FirBasedSymbol<*>>(
    firFile: FirFile,
    val firSymbol: S,
    moduleComponents: LLFirModuleResolveComponents,
) : FileStructureElement(firFile, moduleComponents) {
    abstract override val psi: KtDeclaration
    abstract val timestamp: Long

    /**
     * Creates new declaration by [newKtDeclaration] which will serve as replacement of [firSymbol]
     * Also, modify [firFile] & replace old version of declaration with a new one
     */
    abstract fun reanalyze(
        newKtDeclaration: KT,
    ): ReanalyzableStructureElement<KT, S>

    fun isUpToDate(): Boolean = psi.getModificationStamp() == timestamp

    override val diagnostics = FileStructureElementDiagnostics(
        firFile,
        SingleNonLocalDeclarationDiagnosticRetriever(firSymbol.fir),
        moduleComponents,
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
    moduleComponents: LLFirModuleResolveComponents,
) : ReanalyzableStructureElement<KtNamedFunction, FirFunctionSymbol<*>>(firFile, firSymbol, moduleComponents) {
    override val mappings = KtToFirMapping(firSymbol.fir, recorder)

    override fun reanalyze(newKtDeclaration: KtNamedFunction): ReanalyzableFunctionStructureElement {
        val originalFunction = firSymbol.fir as FirSimpleFunction
        val designation = originalFunction.collectDesignation()

        val temporaryFunction = RawFirNonLocalDeclarationBuilder.buildWithFunctionSymbolRebind(
            session = originalFunction.moduleData.session,
            scopeProvider = originalFunction.moduleData.session.kotlinScopeProvider,
            designation = designation,
            rootNonLocalDeclaration = newKtDeclaration,
        ) as FirSimpleFunction

        return moduleComponents.globalResolveComponents.lockProvider.withLock(firFile) {
            val upgradedPhase = minOf(originalFunction.resolvePhase, FirResolvePhase.DECLARATIONS)

            withInvalidationOnException(moduleComponents.session) {
                with(originalFunction) {
                    replaceBody(temporaryFunction.body)
                    replaceContractDescription(temporaryFunction.contractDescription)
                    @OptIn(ResolveStateAccess::class)
                    resolveState = upgradedPhase.asResolveState()
                }

                designation.toSequence(includeTarget = true).forEach {
                    @OptIn(ResolveStateAccess::class)
                    it.resolveState = minOf(it.resolvePhase, upgradedPhase).asResolveState()
                }

                originalFunction.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)

                ReanalyzableFunctionStructureElement(
                    firFile,
                    newKtDeclaration,
                    originalFunction.symbol,
                    newKtDeclaration.modificationStamp,
                    moduleComponents,
                )
            }
        }
    }
}

internal class ReanalyzablePropertyStructureElement(
    firFile: FirFile,
    override val psi: KtProperty,
    firSymbol: FirPropertySymbol,
    override val timestamp: Long,
    moduleComponents: LLFirModuleResolveComponents,
) : ReanalyzableStructureElement<KtProperty, FirPropertySymbol>(firFile, firSymbol, moduleComponents) {
    override val mappings = KtToFirMapping(firSymbol.fir, recorder)

    override fun reanalyze(newKtDeclaration: KtProperty): ReanalyzablePropertyStructureElement {
        val originalProperty = firSymbol.fir
        val designation = originalProperty.collectDesignation()

        val temporaryProperty = RawFirNonLocalDeclarationBuilder.buildWithFunctionSymbolRebind(
            session = originalProperty.moduleData.session,
            scopeProvider = originalProperty.moduleData.session.kotlinScopeProvider,
            designation = designation,
            rootNonLocalDeclaration = newKtDeclaration,
        ) as FirProperty

        return moduleComponents.globalResolveComponents.lockProvider.withLock(firFile) {
            val getterPhase = originalProperty.getter?.resolvePhase ?: originalProperty.resolvePhase
            val setterPhase = originalProperty.setter?.resolvePhase ?: originalProperty.resolvePhase
            val upgradedPhase = minOf(originalProperty.resolvePhase, getterPhase, setterPhase, FirResolvePhase.DECLARATIONS)

            withInvalidationOnException(moduleComponents.session) {
                @OptIn(ResolveStateAccess::class)
                with(originalProperty) {
                    getter?.replaceBody(temporaryProperty.getter?.body)
                    setter?.replaceBody(temporaryProperty.setter?.body)
                    replaceInitializer(temporaryProperty.initializer)
                    getter?.resolveState = upgradedPhase.asResolveState()
                    setter?.resolveState = upgradedPhase.asResolveState()
                    resolveState = upgradedPhase.asResolveState()
                    replaceBodyResolveState(FirPropertyBodyResolveState.NOTHING_RESOLVED)
                }

                originalProperty.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)

                ReanalyzablePropertyStructureElement(
                    firFile,
                    newKtDeclaration,
                    originalProperty.symbol,
                    newKtDeclaration.modificationStamp,
                    moduleComponents,
                )
            }
        }
    }
}

internal class NonReanalyzableDeclarationStructureElement(
    firFile: FirFile,
    val fir: FirDeclaration,
    override val psi: KtDeclaration,
    moduleComponents: LLFirModuleResolveComponents,
) : FileStructureElement(firFile, moduleComponents) {
    override val mappings = KtToFirMapping(fir, recorder)

    override val diagnostics = FileStructureElementDiagnostics(
        firFile,
        SingleNonLocalDeclarationDiagnosticRetriever(fir),
        moduleComponents,
    )


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

internal class DanglingTopLevelModifierListStructureElement(
    firFile: FirFile,
    val fir: FirDeclaration,
    moduleComponents: LLFirModuleResolveComponents,
    override val psi: KtAnnotated
) :
    FileStructureElement(firFile, moduleComponents) {
    override val mappings = KtToFirMapping(fir, FirElementsRecorder())

    override val diagnostics = FileStructureElementDiagnostics(firFile, SingleNonLocalDeclarationDiagnosticRetriever(fir), moduleComponents)
}

internal class RootStructureElement(
    firFile: FirFile,
    override val psi: KtFile,
    moduleComponents: LLFirModuleResolveComponents,
) : FileStructureElement(firFile, moduleComponents) {
    override val mappings = KtToFirMapping(firFile, recorder)

    override val diagnostics =
        FileStructureElementDiagnostics(firFile, FileDiagnosticRetriever, moduleComponents)

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
