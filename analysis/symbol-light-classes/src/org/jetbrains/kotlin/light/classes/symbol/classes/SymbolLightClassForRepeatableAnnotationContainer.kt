/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.*
import kotlinx.collections.immutable.persistentHashSetOf
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.light.classes.symbol.NullabilityType
import org.jetbrains.kotlin.light.classes.symbol.annotations.SymbolLightSimpleAnnotation
import org.jetbrains.kotlin.light.classes.symbol.annotations.computeAnnotations
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightRepeatableAnnotationContainerMethod
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightClassModifierList
import org.jetbrains.kotlin.load.java.JvmAbi

internal class SymbolLightClassForRepeatableAnnotationContainer(private val containerOwner: SymbolLightClassForAnnotationClass) :
    SymbolLightClassForAnnotationClass(
        containerOwner.classOrObjectDeclaration,
        containerOwner.classOrObjectSymbolPointer,
        containerOwner.ktModule,
        containerOwner.manager,
    ) {
    override fun getQualifiedName(): String? = containerOwner.qualifiedName?.let { "$it.${JvmAbi.REPEATABLE_ANNOTATION_CONTAINER_NAME}" }
    override fun getName(): String = JvmAbi.REPEATABLE_ANNOTATION_CONTAINER_NAME
    override fun getParent(): SymbolLightClassForAnnotationClass = containerOwner
    override fun getTypeParameterList(): PsiTypeParameterList? = null
    override fun getTypeParameters(): Array<PsiTypeParameter> = PsiTypeParameter.EMPTY_ARRAY
    override fun getContainingClass(): KtLightClass = containerOwner
    override fun getScope(): PsiElement = containerOwner
    override fun getOwnInnerClasses(): List<PsiClass> = emptyList()
    override fun getOwnFields(): List<KtLightField> = emptyList()
    override fun isDeprecated(): Boolean = false
    override fun hasTypeParameters(): Boolean = false

    override fun computeModifierList(): PsiModifierList = SymbolLightClassModifierList(
        containingDeclaration = this,
        // It is marked as Abstract because all the annotation classes are marked as Abstract
        // It is marked as Static because all nested interfaces marked as Static
        staticModifiers = setOf(PsiModifier.PUBLIC, PsiModifier.STATIC, PsiModifier.ABSTRACT),
        annotationsComputer = { psiModifierList ->
            val annotations = withClassOrObjectSymbol { classOrObjectSymbol ->
                classOrObjectSymbol.computeAnnotations(
                    modifierList = psiModifierList,
                    nullability = NullabilityType.Unknown,
                    annotationUseSiteTarget = null,
                )
            }.filter {
                it.qualifiedName in allowedAnnotations
            }

            annotations + SymbolLightSimpleAnnotation(fqName = KOTLIN_JVM_INTERNAL_REPEATABLE_CONTAINER, parent = psiModifierList)
        },
    )

    override fun computeOwnMethods(): List<PsiMethod> = listOf(
        SymbolLightRepeatableAnnotationContainerMethod(containerOwner.qualifiedName, this)
    )

    override fun copy(): SymbolLightClassForRepeatableAnnotationContainer = SymbolLightClassForRepeatableAnnotationContainer(containerOwner)

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        return isEquivalentToByName(another)
    }

    override fun equals(other: Any?): Boolean = this === other ||
            other is SymbolLightClassForRepeatableAnnotationContainer && other.containerOwner == containerOwner

    override fun hashCode(): Int = containerOwner.hashCode()

    companion object {
        private val allowedAnnotations = persistentHashSetOf(
            CommonClassNames.JAVA_LANG_ANNOTATION_RETENTION,
            StandardNames.FqNames.retention.asString(),
            CommonClassNames.JAVA_LANG_ANNOTATION_TARGET,
            StandardNames.FqNames.target.asString(),
        )

        private const val KOTLIN_JVM_INTERNAL_REPEATABLE_CONTAINER = "kotlin.jvm.internal.RepeatableContainer"
    }
}
