/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.fields

import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiImmediateClassType
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.asJava.classes.annotateByTypeAnnotationProvider
import org.jetbrains.kotlin.asJava.classes.cannotModify
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.light.classes.symbol.analyzeForLightClasses
import org.jetbrains.kotlin.light.classes.symbol.compareSymbolPointers
import org.jetbrains.kotlin.light.classes.symbol.annotations.GranularAnnotationsBox
import org.jetbrains.kotlin.light.classes.symbol.annotations.LightTypeElementWithParent
import org.jetbrains.kotlin.light.classes.symbol.annotations.SymbolAnnotationsProvider
import org.jetbrains.kotlin.light.classes.symbol.annotations.SymbolLightSimpleAnnotation
import org.jetbrains.kotlin.light.classes.symbol.cachedValue
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForClassOrObject
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForEnumEntry
import org.jetbrains.kotlin.light.classes.symbol.isValid
import org.jetbrains.kotlin.light.classes.symbol.isOriginEquivalentTo
import org.jetbrains.kotlin.light.classes.symbol.psiForLightClasses
import org.jetbrains.kotlin.light.classes.symbol.withSymbol
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.InitializedModifiersBox
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightMemberModifierList
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

internal class SymbolLightFieldForEnumEntry private constructor(
    private val enumEntrySymbolPointer: KaSymbolPointer<KaEnumEntrySymbol>,
    private val enumEntryName: String,
    containingClass: SymbolLightClassForClassOrObject,
    override val kotlinOrigin: KtEnumEntry?,
) : SymbolLightField(containingClass = containingClass, lightMemberOrigin = null), PsiEnumConstant {
    internal constructor(
        enumEntry: KtEnumEntry,
        enumEntryName: String,
        containingClass: SymbolLightClassForClassOrObject,
    ) : this(
        enumEntrySymbolPointer = analyzeForLightClasses(containingClass.ktModule) {
            enumEntry.symbol.createPointer()
        },
        enumEntryName = enumEntryName,
        containingClass = containingClass,
        kotlinOrigin = enumEntry,
    )

    internal constructor(
        enumEntrySymbol: KaEnumEntrySymbol,
        enumEntryName: String,
        containingClass: SymbolLightClassForClassOrObject,
        enumEntry: KtEnumEntry? = enumEntrySymbol.psiForLightClasses(),
    ) : this(
        enumEntrySymbolPointer = enumEntrySymbol.createPointer(),
        enumEntryName = enumEntryName,
        containingClass = containingClass,
        kotlinOrigin = enumEntry,
    )

    internal inline fun <T> withEnumEntrySymbol(crossinline action: KaSession.(KaEnumEntrySymbol) -> T): T =
        enumEntrySymbolPointer.withSymbol(ktModule, action)

    override fun getModifierList(): PsiModifierList = cachedValue {
        SymbolLightMemberModifierList(
            containingDeclaration = this,
            modifiersBox = InitializedModifiersBox(PsiModifier.STATIC, PsiModifier.FINAL, PsiModifier.PUBLIC),
            annotationsBox = GranularAnnotationsBox(
                annotationsProvider = SymbolAnnotationsProvider(
                    ktModule = ktModule,
                    annotatedSymbolPointer = enumEntrySymbolPointer,
                )
            ),
        )
    }

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        return super.isEquivalentTo(another) || isOriginEquivalentTo(another)
    }

    override fun isDeprecated(): Boolean = false

    private val hasBody: Boolean by lazyPub {
        kotlinOrigin?.body != null || withEnumEntrySymbol { enumEntrySymbol ->
            enumEntrySymbol.enumEntryInitializer
                ?.combinedDeclaredMemberScope
                ?.declarations
                ?.any { it !is KaConstructorSymbol } == true
        }
    }

    override fun getInitializingClass(): PsiEnumConstantInitializer? = cachedValue {
        hasBody.ifTrue {
            SymbolLightClassForEnumEntry(
                enumConstant = this@SymbolLightFieldForEnumEntry,
                enumClass = containingClass,
                ktModule = ktModule,
            )
        }
    }

    override fun getOrCreateInitializingClass(): PsiEnumConstantInitializer = initializingClass ?: cannotModify()

    override fun getArgumentList(): PsiExpressionList? = null
    override fun resolveMethod(): PsiMethod? = null
    override fun resolveConstructor(): PsiMethod? = null

    override fun resolveMethodGenerics(): JavaResolveResult = JavaResolveResult.EMPTY

    override fun hasInitializer() = true
    override fun computeConstantValue() = this

    override fun getName(): String = enumEntryName

    override fun getType(): PsiType {
        val psiType = PsiImmediateClassType(containingClass, PsiSubstitutor.EMPTY)
        val typeElement = LightTypeElementWithParent(lightParent = this, psiType)
        psiType.annotateByTypeAnnotationProvider(
            annotations = sequenceOf(
                listOf(SymbolLightSimpleAnnotation(fqName = NotNull::class.java.name, parent = typeElement))
            )
        )

        return psiType
    }

    override fun getInitializer(): PsiExpression? = null

    override fun isValid(): Boolean = super.isValid() && (kotlinOrigin?.isValid ?: enumEntrySymbolPointer.isValid(ktModule))

    override fun hashCode(): Int = kotlinOrigin?.hashCode() ?: enumEntryName.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SymbolLightFieldForEnumEntry || other.ktModule != ktModule) return false
        if (kotlinOrigin != null || other.kotlinOrigin != null) {
            return other.kotlinOrigin == kotlinOrigin
        }

        return other.containingClass == containingClass &&
                compareSymbolPointers(other.enumEntrySymbolPointer, enumEntrySymbolPointer)
    }
}
