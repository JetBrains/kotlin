/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.ClassDiagnosticRetreiver
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.FileDiagnosticRetriever
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.FileStructureElementDiagnostics
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.SingleNonLocalDeclarationDiagnosticRetriever
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.RawFirNonLocalDeclarationBuilder
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
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
        val originalDesignation = originalFunction.collectDesignation()

        val newFunction = RawFirNonLocalDeclarationBuilder.buildWithFunctionSymbolRebind(
            session = originalFunction.moduleData.session,
            scopeProvider = originalFunction.moduleData.session.kotlinScopeProvider,
            designation = originalDesignation,
            rootNonLocalDeclaration = newKtDeclaration,
        ) as FirSimpleFunction

        newFunction.apply {
            copyAllExceptBodyForFunction(originalFunction)
            replaceResolveState(FirResolvePhase.STATUS.asResolveState())
        }

        newFunction.bodyResolveInAir(originalDesignation, firFile, moduleComponents)

        return ReanalyzableFunctionStructureElement(
            firFile,
            newKtDeclaration,
            newFunction.symbol,
            newKtDeclaration.modificationStamp,
            moduleComponents,
        )
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
        val originalDesignation = originalProperty.collectDesignation()

        val newProperty = RawFirNonLocalDeclarationBuilder.buildWithFunctionSymbolRebind(
            session = originalProperty.moduleData.session,
            scopeProvider = originalProperty.moduleData.session.kotlinScopeProvider,
            designation = originalDesignation,
            rootNonLocalDeclaration = newKtDeclaration,
        ) as FirProperty


        newProperty.apply {
            copyAllExceptBodyFromCallable(originalProperty)
            replaceBodyResolveState(FirPropertyBodyResolveState.NOTHING_RESOLVED)
            replaceResolveState(FirResolvePhase.STATUS.asResolveState())
            getter?.let { getter ->
                getter.copyAllExceptBodyForFunction(originalProperty.getter!!)
                getter.replaceResolveState(FirResolvePhase.STATUS.asResolveState())
            }
            setter?.let { setter ->
                setter.copyAllExceptBodyForFunction(originalProperty.setter!!)
                setter.replaceResolveState(FirResolvePhase.STATUS.asResolveState())
            }
        }

        newProperty.bodyResolveInAir(originalDesignation, firFile, moduleComponents)

        return ReanalyzablePropertyStructureElement(
            firFile,
            newKtDeclaration,
            newProperty.symbol,
            newKtDeclaration.modificationStamp,
            moduleComponents,
        )
    }
}

internal sealed class NonReanalyzableDeclarationStructureElement(
    firFile: FirFile,
    moduleComponents: LLFirModuleResolveComponents,
) : FileStructureElement(firFile, moduleComponents)

internal class NonReanalyzableClassDeclarationStructureElement(
    firFile: FirFile,
    val fir: FirRegularClass,
    override val psi: KtClassOrObject,
    moduleComponents: LLFirModuleResolveComponents,
) : NonReanalyzableDeclarationStructureElement(firFile, moduleComponents) {

    override val mappings = KtToFirMapping(fir, Recorder())

    override val diagnostics = FileStructureElementDiagnostics(
        firFile,
        ClassDiagnosticRetreiver(fir),
        moduleComponents,
    )

    private inner class Recorder : FirElementsRecorder() {
        override fun visitProperty(property: FirProperty, data: MutableMap<KtElement, FirElement>) {
            if (property.source?.kind == KtFakeSourceElementKind.PropertyFromParameter) {
                super.visitProperty(property, data)
            }
        }

        override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: MutableMap<KtElement, FirElement>) {
        }

        override fun visitConstructor(constructor: FirConstructor, data: MutableMap<KtElement, FirElement>) {
            if (constructor is FirPrimaryConstructor && constructor.source?.kind == KtFakeSourceElementKind.ImplicitConstructor) {
                super.visitConstructor(constructor, data)
            }
        }

        override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer, data: MutableMap<KtElement, FirElement>) {
        }

        override fun visitRegularClass(regularClass: FirRegularClass, data: MutableMap<KtElement, FirElement>) {
            if (regularClass != fir) return
            super.visitRegularClass(regularClass, data)
        }

        override fun visitTypeAlias(typeAlias: FirTypeAlias, data: MutableMap<KtElement, FirElement>) {
        }
    }
}

internal class NonReanalyzableNonClassDeclarationStructureElement(
    firFile: FirFile,
    val fir: FirDeclaration,
    override val psi: KtDeclaration,
    moduleComponents: LLFirModuleResolveComponents,
) : NonReanalyzableDeclarationStructureElement(firFile, moduleComponents) {

    override val mappings = KtToFirMapping(fir, Recorder)

    override val diagnostics = FileStructureElementDiagnostics(
        firFile,
        SingleNonLocalDeclarationDiagnosticRetriever(fir),
        moduleComponents,
    )

    private object Recorder : FirElementsRecorder()
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

private fun <C : FirCallableDeclaration> C.bodyResolveInAir(
    originalDesignation: FirDesignation,
    firFile: FirFile,
    moduleComponents: LLFirModuleResolveComponents
) {
    val designationToResolveOnAir = FirDesignationWithFile(originalDesignation.path, this, firFile)
    moduleComponents.firModuleLazyDeclarationResolver.runLazyDesignatedOnAirResolveToBodyWithoutLock(
        designationToResolveOnAir,
        onAirCreatedDeclaration = false,
        towerDataContextCollector = null
    )
}


private fun <F : FirFunction> F.copyAllExceptBodyForFunction(prototype: F) {
    this.copyAllExceptBodyFromCallable(prototype)
    this.replaceValueParameters(prototype.valueParameters)

    if (this is FirContractDescriptionOwner) {
        this.replaceContractDescription((prototype as FirContractDescriptionOwner).contractDescription)
    }
}

private fun <C : FirCallableDeclaration> C.copyAllExceptBodyFromCallable(prototype: C) {
    this.replaceAnnotations(prototype.annotations)
    this.replaceReturnTypeRef(prototype.returnTypeRef)
    this.replaceReceiverParameter(prototype.receiverParameter)
    this.replaceStatus(prototype.status)
    this.replaceDeprecationsProvider(prototype.deprecationsProvider)
    this.replaceContextReceivers(prototype.contextReceivers)
    this.replaceDispatchReceiverType(prototype.dispatchReceiverType)
    this.replaceAttributes(prototype.attributes)

    // TODO add replaceTypeParameter to FirCallableDeclaration instead of this unsafe case
    (this.typeParameters as MutableList<FirTypeParameterRef>).apply {
        clear()
        addAll(prototype.typeParameters)
    }
}