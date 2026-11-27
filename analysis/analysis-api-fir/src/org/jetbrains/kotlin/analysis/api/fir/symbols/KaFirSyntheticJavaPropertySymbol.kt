/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.KaInitializerValue
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirJavaSyntheticPropertySymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.createOwnerPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.resolve.isSubclassOf
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.SyntheticSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirSyntheticPropertySymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

internal class KaFirSyntheticJavaPropertySymbol(
    override val firSymbol: FirSyntheticPropertySymbol,
    override val analysisSession: KaFirSession,
) : KaSyntheticJavaPropertySymbol(), KaFirSymbol<FirSyntheticPropertySymbol> {
    override val isVal: Boolean get() = withValidityAssertion { firSymbol.isVal }
    override val name: Name get() = withValidityAssertion { firSymbol.name }
    override val isActual: Boolean get() = withValidityAssertion { firSymbol.isActual }
    override val isExpect: Boolean get() = withValidityAssertion { firSymbol.isExpect }
    override val returnType: KaType get() = withValidityAssertion { firSymbol.returnType(builder) }

    override val receiverParameter: KaReceiverParameterSymbol?
        get() = withValidityAssertion { KaFirReceiverParameterSymbol.create(null, this, analysisSession = analysisSession) }

    override val typeParameters: List<KaTypeParameterSymbol>
        get() = withValidityAssertion { firSymbol.createKtTypeParameters(builder) }

    override val isExtension: Boolean
        get() = withValidityAssertion { false }

    override val origin: KaSymbolOrigin
        get() = withValidityAssertion { KaSymbolOrigin.JAVA_SYNTHETIC_PROPERTY }

    override val initializer: KaInitializerValue? get() = withValidityAssertion { firSymbol.getKtConstantInitializer(builder) }

    override val modality: KaSymbolModality get() = withValidityAssertion { firSymbol.kaSymbolModality }
    override val visibility: KaSymbolVisibility get() = withValidityAssertion { firSymbol.visibility.asKaSymbolVisibility }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion {
            KaFirAnnotationListForDeclaration.create(firSymbol, builder)
        }

    override val callableId: CallableId?
        get() = withValidityAssertion { firSymbol.getCallableId() }

    override val getter: KaPropertyGetterSymbol
        get() = withValidityAssertion {
            KaFirSyntheticPropertyGetterSymbol(this)
        }

    override val javaGetterSymbol: KaNamedFunctionSymbol
        get() = withValidityAssertion {
            val fir = firSymbol.fir as FirSyntheticProperty
            return builder.functionBuilder.buildNamedFunctionSymbol(fir.getter.delegate.symbol)
        }

    override val javaSetterSymbol: KaNamedFunctionSymbol?
        get() = withValidityAssertion {
            val fir = firSymbol.fir as FirSyntheticProperty
            return fir.setter?.delegate?.let { builder.functionBuilder.buildNamedFunctionSymbol(it.symbol) }
        }

    override val setter: KaPropertySetterSymbol?
        get() = withValidityAssertion {
            firSymbol.setterSymbol?.let {
                KaFirSyntheticPropertySetterSymbol(this)
            }
        }

    override val isOverride: Boolean get() = withValidityAssertion { firSymbol.isOverride }
    override val isStatic: Boolean get() = withValidityAssertion { firSymbol.isStatic }
    override val isCompanion: Boolean get() = withValidityAssertion { firSymbol.isStatic }

    override val isExternal: Boolean get() = withValidityAssertion { firSymbol.isEffectivelyExternal(analysisSession.firSession) }

    override val hasSetter: Boolean get() = withValidityAssertion { firSymbol.setterSymbol != null }

    override fun createPointer(): KaSymbolPointer<KaSyntheticJavaPropertySymbol> = withValidityAssertion {
        KaFirJavaSyntheticPropertySymbolPointer(
            ownerPointer = createSyntheticPropertyOwnerPointer(),
            propertyName = name,
            isSynthetic = firSymbol is SyntheticSymbol,
            originalSymbol = this
        )
    }

    /**
     * Returns the pointer of the class to restore this property from.
     *
     * A property from the synthetic properties scope is contained in the class of its getter. If its setter comes from a subclass of
     * that class, the scope of the getter's class has another setter or none, so the pointer takes the class of the setter.
     *
     * For example, `foo` has `Derived.setFoo` in the scope of `Derived` and `Base.setFoo` in the scope of `Base`:
     *
     * ```java
     * public class Base {
     *     public String getFoo() { return ""; }
     *     public void setFoo(String value) {}
     * }
     *
     * public class Derived extends Base {
     *     @Override
     *     public void setFoo(String value) {}
     * }
     * ```
     */
    private fun createSyntheticPropertyOwnerPointer(): KaSymbolPointer<KaDeclarationContainerSymbol> {
        // A property from the member scope of a Java class is contained in that class, whose member scope has both accessors
        if (firSymbol !is SyntheticSymbol) return createOwnerPointer()

        val firSession = analysisSession.firSession
        val getterClass = firSymbol.getterSymbol?.delegateFunctionSymbol?.containingClassLookupTag()
        val setterClass = firSymbol.setterSymbol?.delegateFunctionSymbol?.containingClassLookupTag()?.toRegularClassSymbol(firSession)
        val isSetterFromSubclass = getterClass != null && setterClass != null &&
                setterClass.isSubclassOf(getterClass, firSession, isStrict = true, lookupInterfaces = true)

        return if (isSetterFromSubclass) {
            builder.classifierBuilder.buildNamedClassSymbol(setterClass).createPointer()
        } else {
            // The class of the getter, which is the containing declaration
            createOwnerPointer()
        }
    }

    /**
     * Synthetic properties are built anew on each scope lookup, so two symbols of the same property usually have different
     * [firSymbol]s. The property is defined by its name and its Java accessors, which are stable.
     *
     * This is a workaround for KT-91296: once the compiler reuses synthetic properties, [symbolEquals] is enough.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KaFirSyntheticJavaPropertySymbol) return false
        if (firSymbol == other.firSymbol) return true

        return firSymbol.name == other.firSymbol.name &&
                firSymbol.getterSymbol?.delegateFunctionSymbol == other.firSymbol.getterSymbol?.delegateFunctionSymbol &&
                firSymbol.setterSymbol?.delegateFunctionSymbol == other.firSymbol.setterSymbol?.delegateFunctionSymbol
    }

    override fun hashCode(): Int = firSymbol.getterSymbol?.delegateFunctionSymbol.hashCode()
}
