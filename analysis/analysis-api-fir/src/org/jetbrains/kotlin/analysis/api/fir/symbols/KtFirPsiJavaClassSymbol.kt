/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

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
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.analysis.project.structure.getKtModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.firClassByPsiClassProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.analysis.utils.classIdIfNonLocal
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.java.classKind
import org.jetbrains.kotlin.fir.java.modality
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl

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

    override val classIdIfNonLocal: ClassId? by cached { psi.classIdIfNonLocal }

    override val origin: KtSymbolOrigin
        get() = withValidityAssertion { KtSymbolOrigin.JAVA }

    override val symbolKind: KtSymbolKind
        get() = withValidityAssertion {
            val classId = classIdIfNonLocal
            when {
                classId == null -> KtSymbolKind.LOCAL
                classId.outerClassId != null -> KtSymbolKind.CLASS_MEMBER
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
        get() = withValidityAssertion { classIdIfNonLocal?.outerClassId != null && !javaClass.isStatic }

    override val isData: Boolean get() = withValidityAssertion { false }
    override val isInline: Boolean get() = withValidityAssertion { false }
    override val isFun: Boolean get() = withValidityAssertion { false }
    override val isExternal: Boolean get() = withValidityAssertion { false }
    override val companionObject: KtNamedClassOrObjectSymbol? get() = withValidityAssertion { null }

    override val contextReceivers: List<KtContextReceiver> get() = withValidityAssertion { emptyList() }

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
        firSymbol.createRegularKtTypeParameters(builder)
    }

    override val annotationsList: KtAnnotationsList by cached {
        if (javaClass.annotations.isEmpty()) KtEmptyAnnotationsList(token)
        else KtFirAnnotationListForDeclaration.create(firSymbol, analysisSession.useSiteSession, token)
    }

    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtNamedClassOrObjectSymbol> = withValidityAssertion {
        createNamedClassOrObjectSymbolPointer()
    }

    // TODO (marco): Can we implement equality and hash code without falling back to the symbol?
    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}
