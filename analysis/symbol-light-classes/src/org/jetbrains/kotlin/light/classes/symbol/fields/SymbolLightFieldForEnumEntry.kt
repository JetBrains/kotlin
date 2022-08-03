/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.fields

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.lifetime.isValid
import org.jetbrains.kotlin.analysis.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.cannotModify
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.light.classes.symbol.SymbolLightIdentifier
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClass
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForEnumEntry
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightMemberModifierList
import org.jetbrains.kotlin.light.classes.symbol.nonExistentType
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.light.classes.symbol.NullabilityType
import org.jetbrains.kotlin.light.classes.symbol.annotations.computeAnnotations
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

context(KtAnalysisSession)
internal class SymbolLightFieldForEnumEntry(
    private val enumEntrySymbol: KtEnumEntrySymbol,
    containingClass: SymbolLightClass,
    override val lightMemberOrigin: LightMemberOrigin?
) : SymbolLightField(containingClass, lightMemberOrigin), PsiEnumConstant {

    private val _modifierList by lazyPub {
        SymbolLightMemberModifierList(
            containingDeclaration = this@SymbolLightFieldForEnumEntry,
            modifiers = setOf(PsiModifier.STATIC, PsiModifier.FINAL, PsiModifier.PUBLIC),
            annotations = enumEntrySymbol.computeAnnotations(
                this,
                nullability = NullabilityType.Unknown, // there is no need to add nullability annotations on enum entries
                annotationUseSiteTarget = AnnotationUseSiteTarget.FIELD
            )
        )
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    override val kotlinOrigin: KtEnumEntry? = enumEntrySymbol.psi as? KtEnumEntry

    override fun isDeprecated(): Boolean = false

    //TODO Make with KtSymbols
    private val hasBody: Boolean get() = kotlinOrigin?.let { it.body != null } ?: true

    private val _initializingClass: PsiEnumConstantInitializer? by lazyPub {
        hasBody.ifTrue {
            SymbolLightClassForEnumEntry(
                enumEntrySymbol = enumEntrySymbol,
                enumConstant = this@SymbolLightFieldForEnumEntry,
                enumClass = containingClass,
                manager = manager
            )
        }
    }

    override fun getInitializingClass(): PsiEnumConstantInitializer? = _initializingClass
    override fun getOrCreateInitializingClass(): PsiEnumConstantInitializer =
        _initializingClass ?: cannotModify()

    override fun getArgumentList(): PsiExpressionList? = null
    override fun resolveMethod(): PsiMethod? = null
    override fun resolveConstructor(): PsiMethod? = null

    override fun resolveMethodGenerics(): JavaResolveResult = JavaResolveResult.EMPTY

    override fun hasInitializer() = true
    override fun computeConstantValue(visitedVars: MutableSet<PsiVariable>?) = this

    override fun getName(): String = enumEntrySymbol.name.asString()

    private val _type: PsiType by lazyPub {
        enumEntrySymbol.returnType.asPsiType(this@SymbolLightFieldForEnumEntry) ?: nonExistentType()
    }

    override fun getType(): PsiType = _type
    override fun getInitializer(): PsiExpression? = null

    override fun hashCode(): Int = enumEntrySymbol.hashCode()

    private val _identifier: PsiIdentifier by lazyPub {
        SymbolLightIdentifier(this, enumEntrySymbol)
    }

    override fun getNameIdentifier(): PsiIdentifier = _identifier

    override fun isValid(): Boolean = super.isValid() && enumEntrySymbol.isValid()


    override fun equals(other: Any?): Boolean = other is SymbolLightFieldForEnumEntry && enumEntrySymbol == other.enumEntrySymbol
}
