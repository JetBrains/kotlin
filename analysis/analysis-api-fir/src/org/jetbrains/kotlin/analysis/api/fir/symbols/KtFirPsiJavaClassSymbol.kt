/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.codeInsight.PsiEquivalenceUtil
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.base.KtContextReceiver
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KtFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KtEmptyAnnotationsList
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.toKtClassKind
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.firClassByPsiClassProvider
import org.jetbrains.kotlin.analysis.project.structure.getKtModule
import org.jetbrains.kotlin.analysis.utils.classIdIfNonLocal
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.java.classKind
import org.jetbrains.kotlin.fir.java.modality
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

/**
 * Implements [KtNamedClassOrObjectSymbol] for a Java class. The underlying [firSymbol] is built lazily and only when needed. Many simple
 * properties are computed from the given [PsiClass] instead of [firSymbol]. This improves performance when "slow" properties don't need to
 * be accessed.
 */
internal class KtFirPsiJavaClassSymbol(
    override val psi: PsiClass,
    override val analysisSession: KtFirAnalysisSession,
) : KtNamedClassOrObjectSymbol(), KtFirPsiSymbol<PsiClass, FirRegularClassSymbol> {
    /**
     * [javaClass] is used to defer some properties to the compiler's view of a Java class.
     */
    private val javaClass: JavaClass = JavaClassImpl(psi)

    override val name: Name = withValidityAssertion { javaClass.name }

    override val classIdIfNonLocal: ClassId = withValidityAssertion {
        psi.classIdIfNonLocal ?: error("${KtFirPsiJavaClassSymbol::class.simpleName} requires a non-local PSI class.")
    }

    override val origin: KtSymbolOrigin
        get() = withValidityAssertion { KtSymbolOrigin.JAVA }

    override val symbolKind: KtSymbolKind
        get() = withValidityAssertion {
            when {
                classIdIfNonLocal.outerClassId != null -> KtSymbolKind.CLASS_MEMBER
                else -> KtSymbolKind.TOP_LEVEL
            }
        }

    @OptIn(KtAnalysisApiInternals::class)
    override val classKind: KtClassKind
        get() = withValidityAssertion { javaClass.classKind.toKtClassKind(isCompanionObject = false) }

    override val modality: Modality
        get() = withValidityAssertion { javaClass.modality }

    override val visibility: Visibility
        get() = withValidityAssertion { javaClass.visibility }

    override val isInner: Boolean
        get() = withValidityAssertion { classIdIfNonLocal.outerClassId != null && !javaClass.isStatic }

    val outerClass: KtFirPsiJavaClassSymbol?
        get() = withValidityAssertion {
            psi.containingClass?.let { KtFirPsiJavaClassSymbol(it, analysisSession) }
        }

    override val typeParameterNames: List<Name> by cached {
        // The parent Java class might contribute `FirOuterClassTypeParameterRef`s to the FIR class's type parameters (see
        // `FirJavaFacade.createFirJavaClass`), but since they are filtered out by `createRegularKtTypeParameters`, we do not need to
        // include them in the list of type parameter names.
        javaClass.typeParameters.map { it.name }
    }

    val hasTypeParameters: Boolean
        get() = withValidityAssertion { psi.typeParameters.isNotEmpty() }

    val annotationSimpleNames: List<String?>
        get() = withValidityAssertion { psi.annotations.map { it.nameReferenceElement?.referenceName } }

    val hasAnnotations: Boolean
        get() = withValidityAssertion { psi.annotations.isNotEmpty() }

    override val isData: Boolean get() = withValidityAssertion { false }
    override val isInline: Boolean get() = withValidityAssertion { false }
    override val isFun: Boolean get() = withValidityAssertion { false }
    override val isExternal: Boolean get() = withValidityAssertion { false }
    override val companionObject: KtNamedClassOrObjectSymbol? get() = withValidityAssertion { null }

    override val contextReceivers: List<KtContextReceiver> get() = withValidityAssertion { emptyList() }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other == null || other !is KtNamedClassOrObjectSymbol) return false

        val otherPsiClass = other.psi as? PsiClass ?: return false
        return PsiEquivalenceUtil.areElementsEquivalent(psi, otherPsiClass)
    }

    /**
     * Since [PsiEquivalenceUtil] doesn't have a hash code function, we use the class ID as a conservative hash code.
     *
     * Also see [KtFirNamedClassOrObjectSymbol.hashCode], which must generate the same hash code for non-local classes.
     */
    override fun hashCode(): Int = classIdIfNonLocal.hashCode()

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Slow Operations (requiring access to the underlying FIR class symbol)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    override val firSymbol: FirRegularClassSymbol by cached {
        val module = psi.getKtModule(analysisSession.project)
        val provider = analysisSession.firResolveSession.getSessionFor(module).firClassByPsiClassProvider
        val firClassSymbol = provider.getFirClass(psi)

        require(firClassSymbol != null) {
            "A FIR class symbol should be available for ${KtFirPsiJavaClassSymbol::class.simpleName} `$classIdIfNonLocal`."
        }
        firClassSymbol
    }

    override val superTypes: List<KtType> by cached {
        firSymbol.superTypesAndAnnotationsListForRegularClass(builder)
    }

    override val typeParameters: List<KtTypeParameterSymbol> by cached {
        if (hasTypeParameters) firSymbol.createRegularKtTypeParameters(builder)
        else emptyList()
    }

    override val annotationsList: KtAnnotationsList by cached {
        if (hasAnnotations) KtFirAnnotationListForDeclaration.create(firSymbol, analysisSession.useSiteSession, token)
        else KtEmptyAnnotationsList(token)
    }

    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtNamedClassOrObjectSymbol> = withValidityAssertion {
        createNamedClassOrObjectSymbolPointer()
    }
}
