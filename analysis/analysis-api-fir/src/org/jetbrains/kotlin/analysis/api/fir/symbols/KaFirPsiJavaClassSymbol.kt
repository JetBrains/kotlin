/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.getModule
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaBaseEmptyAnnotationList
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.asKaSymbolModality
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.toKtClassKind
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.firClassByPsiClassProvider
import org.jetbrains.kotlin.analysis.utils.classId
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.java.classKind
import org.jetbrains.kotlin.fir.java.modality
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementSourceFactory
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

/**
 * Implements [KaNamedClassSymbol] for a Java class. The underlying [firSymbol] is built lazily and only when needed. Many simple
 * properties are computed from the given [PsiClass] instead of [firSymbol]. This improves performance when "slow" properties don't need to
 * be accessed.
 */
internal class KaFirPsiJavaClassSymbol(
    override val backingPsi: PsiClass,
    override val analysisSession: KaFirSession,
) : KaFirNamedClassSymbolBase<PsiClass>() {
    /**
     * [javaClass] is used to defer some properties to the compiler's view of a Java class.
     */
    private val javaClass: JavaClass = JavaClassImpl(
        JavaElementSourceFactory.getInstance(analysisSession.project).createPsiSource(backingPsi)
    )

    override val psi: PsiElement? get() = withValidityAssertion { backingPsi }

    override val name: Name get() = withValidityAssertion { javaClass.name }

    override val classId: ClassId
        get() = withValidityAssertion {
            backingPsi.classId ?: error("${KaFirPsiJavaClassSymbol::class.simpleName} requires a non-local PSI class.")
        }

    override val origin: KaSymbolOrigin
        get() = withValidityAssertion {
            if (javaClass.isFromSource) KaSymbolOrigin.JAVA_SOURCE else KaSymbolOrigin.JAVA_LIBRARY
        }

    override val location: KaSymbolLocation
        get() = withValidityAssertion {
            when {
                classId.outerClassId != null -> KaSymbolLocation.CLASS
                else -> KaSymbolLocation.TOP_LEVEL
            }
        }

    override val classKind: KaClassKind
        get() = withValidityAssertion { javaClass.classKind.toKtClassKind(isCompanionObject = false) }

    override val modality: KaSymbolModality
        get() = withValidityAssertion { javaClass.modality.asKaSymbolModality }

    override val compilerVisibility: Visibility
        get() = withValidityAssertion { javaClass.visibility }

    override val isInner: Boolean
        get() = withValidityAssertion { classId.outerClassId != null && !javaClass.isStatic }

    val outerClass: KaFirPsiJavaClassSymbol?
        get() = withValidityAssertion { backingPsi.containingClass?.let { KaFirPsiJavaClassSymbol(it, analysisSession) } }

    override val typeParameters: List<KaTypeParameterSymbol>
        get() = withValidityAssertion {
            // The parent Java class might contribute type parameters to the Java type parameter stack, but for this KtSymbol, parent type
            // parameters aren't relevant.
            backingPsi.typeParameters.mapIndexed { index, psiTypeParameter ->
                KaFirPsiJavaTypeParameterSymbol(psiTypeParameter, analysisSession, origin) {
                    // `psi.typeParameters` should align with the list of regular `FirTypeParameter`s, making the use of `index` valid.
                    val firTypeParameter = firSymbol.fir.typeParameters.filterIsInstance<FirTypeParameter>().getOrNull(index)
                    require(firTypeParameter != null) {
                        "The FIR symbol's ${FirTypeParameter::class.simpleName}s should have an entry at $index."
                    }
                    firTypeParameter.symbol
                }
            }
        }

    val annotationSimpleNames: List<String?>
        get() = withValidityAssertion { backingPsi.annotations.map { it.nameReferenceElement?.referenceName } }

    val hasAnnotations: Boolean
        get() = withValidityAssertion { backingPsi.annotations.isNotEmpty() }

    override val isData: Boolean get() = withValidityAssertion { false }
    override val isInline: Boolean get() = withValidityAssertion { false }
    override val isFun: Boolean get() = withValidityAssertion { false }
    override val isExternal: Boolean get() = withValidityAssertion { false }
    override val isActual: Boolean get() = withValidityAssertion { false }
    override val isExpect: Boolean get() = withValidityAssertion { false }

    override val companionObject: KaNamedClassSymbol? get() = withValidityAssertion { null }

    override val contextReceivers: List<KaContextReceiver> get() = withValidityAssertion { emptyList() }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Slow Operations (requiring access to the underlying FIR class symbol)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    override val lazyFirSymbol: Lazy<FirRegularClassSymbol> = lazyPub {
        val module = analysisSession.getModule(backingPsi)
        val provider = analysisSession.resolutionFacade.getSessionFor(module).firClassByPsiClassProvider
        provider.getFirClass(backingPsi)
    }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion {
            if (hasAnnotations) KaFirAnnotationListForDeclaration.create(firSymbol, builder)
            else KaBaseEmptyAnnotationList(token)
        }

    override val superTypes: List<KaType>
        get() = withValidityAssertion {
            firSymbol.superTypesList(builder)
        }
}
