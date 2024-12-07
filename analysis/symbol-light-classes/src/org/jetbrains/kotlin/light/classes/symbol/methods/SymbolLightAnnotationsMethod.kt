/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.methods

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.sourcePsiSafe
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_FOR_ANNOTATIONS
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightIdentifier
import org.jetbrains.kotlin.light.classes.symbol.*
import org.jetbrains.kotlin.light.classes.symbol.annotations.DeprecatedAdditionalAnnotationsProvider
import org.jetbrains.kotlin.light.classes.symbol.annotations.GranularAnnotationsBox
import org.jetbrains.kotlin.light.classes.symbol.annotations.SymbolAnnotationsProvider
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassBase
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.GranularModifiersBox
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightMemberModifierList
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightParameterForReceiver
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightParameterList
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightTypeParameterList
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

internal class SymbolLightAnnotationsMethod private constructor(
    lightMemberOrigin: LightMemberOrigin?,
    containingClass: SymbolLightClassBase,
    private val containingPropertyDeclaration: KtCallableDeclaration?,
    private val containingPropertySymbolPointer: KaSymbolPointer<KaPropertySymbol>,
) : SymbolLightMethodBase(
    lightMemberOrigin,
    containingClass,
    METHOD_INDEX_FOR_ANNOTATIONS,
) {
    internal constructor(
        ktAnalysisSession: KaSession,
        containingPropertySymbol: KaPropertySymbol,
        lightMemberOrigin: LightMemberOrigin?,
        containingClass: SymbolLightClassBase,
    ) : this(
        lightMemberOrigin,
        containingClass,
        containingPropertyDeclaration = containingPropertySymbol.sourcePsiSafe(),
        containingPropertySymbolPointer = with(ktAnalysisSession) { containingPropertySymbol.createPointer() },
    )

    private fun KaSession.propertySymbol(): KaPropertySymbol {
        return restoreSymbolOrThrowIfDisposed(containingPropertySymbolPointer)
    }

    private fun String.abiName(): String {
        return JvmAbi.getSyntheticMethodNameForAnnotatedProperty(JvmAbi.getterName(this))
    }

    private val _name: String by lazyPub {
        analyzeForLightClasses(ktModule) {
            val symbol = propertySymbol()
            val outerClass = this@SymbolLightAnnotationsMethod.containingClass
            val defaultName = symbol.name.identifier.let {
                if (outerClass.isAnnotationType) it else it.abiName()
            }

            computeJvmMethodName(symbol = symbol, defaultName = defaultName)
        }
    }

    override fun getName(): String = _name

    override fun isVarArgs(): Boolean = false

    override val kotlinOrigin: KtDeclaration? get() = containingPropertyDeclaration

    private val _modifierList: PsiModifierList by lazyPub {
        containingPropertySymbolPointer.withSymbol(ktModule) { propertySymbol ->
            SymbolLightMemberModifierList(
                containingDeclaration = this@SymbolLightAnnotationsMethod,
                modifiersBox = GranularModifiersBox(mapOf(PsiModifier.STATIC to true)) { modifier ->
                    when (modifier) {
                        in GranularModifiersBox.VISIBILITY_MODIFIERS -> GranularModifiersBox.computeVisibilityForMember(
                            ktModule,
                            containingPropertySymbolPointer,
                        )
                        else -> null
                    }
                },
                annotationsBox = GranularAnnotationsBox(
                    annotationsProvider = SymbolAnnotationsProvider(
                        ktModule = ktModule,
                        annotatedSymbolPointer = containingPropertySymbolPointer,
                    ),
                    additionalAnnotationsProvider = DeprecatedAdditionalAnnotationsProvider
                ),
            )
        }
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    override fun isConstructor(): Boolean = false

    override fun isDeprecated(): Boolean = true

    private val _identifier: PsiIdentifier by lazyPub {
        KtLightIdentifier(this, containingPropertyDeclaration)
    }

    override fun getNameIdentifier(): PsiIdentifier = _identifier

    override fun getReturnType(): PsiType = PsiTypes.voidType()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SymbolLightAnnotationsMethod) return false
        return other.ktModule == ktModule && containingPropertyDeclaration == other.containingPropertyDeclaration
    }

    override fun hashCode(): Int = containingPropertyDeclaration.hashCode()
    override fun hasTypeParameters(): Boolean = false
    override fun getTypeParameterList(): PsiTypeParameterList? = null
    override fun getTypeParameters(): Array<PsiTypeParameter> = PsiTypeParameter.EMPTY_ARRAY

    internal fun getPropertyTypeParameters(): Array<PsiTypeParameter> =
        _propertyTypeParameterList?.typeParameters ?: PsiTypeParameter.EMPTY_ARRAY

    private val _propertyTypeParameterList: PsiTypeParameterList? by lazyPub {
        propertyHasTypeParameters().ifTrue {
            SymbolLightTypeParameterList(
                owner = this,
                symbolWithTypeParameterPointer = containingPropertySymbolPointer,
                ktModule = ktModule,
                ktDeclaration = containingPropertyDeclaration,
            )
        }
    }

    private fun propertyHasTypeParameters(): Boolean =
        hasTypeParameters(ktModule, containingPropertyDeclaration, containingPropertySymbolPointer)

    private val _parametersList by lazyPub {
        SymbolLightParameterList(
            parent = this@SymbolLightAnnotationsMethod,
            parameterPopulator = { builder ->
                SymbolLightParameterForReceiver.tryGet(
                    callableSymbolPointer = containingPropertySymbolPointer,
                    method = this@SymbolLightAnnotationsMethod
                )?.let(builder::addParameter)
            },
        )
    }

    override fun getParameterList(): PsiParameterList = _parametersList

    override fun isValid(): Boolean =
        super.isValid() && containingPropertySymbolPointer.isValid(ktModule)

    override fun isOverride(): Boolean = false

    override fun getText(): String {
        return lightMemberOrigin?.auxiliaryOriginalElement?.text ?: super.getText()
    }

    override fun getTextOffset(): Int {
        return lightMemberOrigin?.auxiliaryOriginalElement?.textOffset ?: super.getTextOffset()
    }

    override fun getTextRange(): TextRange {
        return lightMemberOrigin?.auxiliaryOriginalElement?.textRange ?: super.getTextRange()
    }
}