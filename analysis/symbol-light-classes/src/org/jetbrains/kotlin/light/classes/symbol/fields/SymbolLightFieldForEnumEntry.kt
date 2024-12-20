/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.fields

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiSymbolPointerCreator
import org.jetbrains.kotlin.asJava.classes.cannotModify
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.light.classes.symbol.analyzeForLightClasses
import org.jetbrains.kotlin.light.classes.symbol.annotations.GranularAnnotationsBox
import org.jetbrains.kotlin.light.classes.symbol.annotations.SymbolAnnotationsProvider
import org.jetbrains.kotlin.light.classes.symbol.cachedValue
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForClassOrObject
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForEnumEntry
import org.jetbrains.kotlin.light.classes.symbol.isOriginEquivalentTo
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.InitializedModifiersBox
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightMemberModifierList
import org.jetbrains.kotlin.light.classes.symbol.nonExistentType
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

internal class SymbolLightFieldForEnumEntry(
    private val enumEntry: KtEnumEntry,
    private val enumEntryName: String,
    containingClass: SymbolLightClassForClassOrObject,
) : SymbolLightField(containingClass = containingClass, lightMemberOrigin = null), PsiEnumConstant {
    internal inline fun <T> withEnumEntrySymbol(crossinline action: KaSession.(KaEnumEntrySymbol) -> T): T =
        analyzeForLightClasses(ktModule) {
            action(enumEntry.symbol)
        }

    @OptIn(KaImplementationDetail::class)
    private val _modifierList by lazyPub {
        SymbolLightMemberModifierList(
            containingDeclaration = this,
            modifiersBox = InitializedModifiersBox(PsiModifier.STATIC, PsiModifier.FINAL, PsiModifier.PUBLIC),
            annotationsBox = GranularAnnotationsBox(
                annotationsProvider = SymbolAnnotationsProvider(
                    ktModule = ktModule,
                    annotatedSymbolPointer = KaPsiSymbolPointerCreator.symbolPointerOfType<KaEnumEntrySymbol>(enumEntry),
                )
            ),
        )
    }

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        return super.isEquivalentTo(another) || isOriginEquivalentTo(another)
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    override val kotlinOrigin: KtEnumEntry = enumEntry

    override fun isDeprecated(): Boolean = false

    private val hasBody: Boolean get() = enumEntry.body != null

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

    private val _type: PsiType by lazyPub {
        withEnumEntrySymbol { enumEntrySymbol ->
            enumEntrySymbol.returnType
                .asPsiType(
                    this@SymbolLightFieldForEnumEntry,
                    allowErrorTypes = true,
                    allowNonJvmPlatforms = true,
                )
                ?: nonExistentType()
        }
    }

    override fun getType(): PsiType = _type
    override fun getInitializer(): PsiExpression? = null

    override fun isValid(): Boolean = enumEntry.isValid

    override fun hashCode(): Int = enumEntry.hashCode()

    override fun equals(other: Any?): Boolean = other is SymbolLightFieldForEnumEntry && enumEntry == other.enumEntry
}
