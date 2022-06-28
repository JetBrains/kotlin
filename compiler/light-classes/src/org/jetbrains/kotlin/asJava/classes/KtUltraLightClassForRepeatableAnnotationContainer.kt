/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.*
import com.intellij.psi.impl.light.LightMethodBuilder
import com.intellij.psi.impl.light.LightModifierList
import com.intellij.psi.impl.light.LightParameterListBuilder
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.asJava.elements.*
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.KtClassOrObject

class KtUltraLightClassForRepeatableAnnotationContainer(classOrObject: KtClassOrObject, support: KtUltraLightSupport) :
    KtUltraLightClass(classOrObject, support) {
    override fun getQualifiedName(): String? = containingClass?.qualifiedName?.let { "$it.${JvmAbi.REPEATABLE_ANNOTATION_CONTAINER_NAME}" }
    override fun getName() = JvmAbi.REPEATABLE_ANNOTATION_CONTAINER_NAME
    override fun getParent() = containingClass
    override fun getTypeParameterList(): PsiTypeParameterList? = null
    override fun getTypeParameters(): Array<PsiTypeParameter> = emptyArray()
    override fun getContainingClass(): KtLightClassForSourceDeclaration? = create(classOrObject, jvmDefaultMode)
    override fun getScope(): PsiElement? = containingClass
    override fun getOwnInnerClasses() = emptyList<PsiClass>()
    override fun getOwnFields(): List<KtLightField> = emptyList()
    override fun getOwnMethods(): List<KtLightMethod> = _ownMethods
    override fun getModifierList(): PsiModifierList? = _modifierList
    override fun isInterface(): Boolean = true
    override fun isDeprecated(): Boolean = false
    override fun isAnnotationType(): Boolean = true
    override fun isEnum(): Boolean = false
    override fun isFinal(isFinalByPsi: Boolean): Boolean = false
    override fun hasTypeParameters(): Boolean = false

    override fun copy(): KtUltraLightClassForRepeatableAnnotationContainer = KtUltraLightClassForRepeatableAnnotationContainer(
        classOrObject.copy() as KtClassOrObject,
        support,
    )

    private val _modifierList: PsiModifierList? by lazyPub {
        KtUltraLightModifierListForRepeatableAnnotationContainer(this, support)
    }

    override fun isInheritor(baseClass: PsiClass, checkDeep: Boolean): Boolean =
        baseClass.qualifiedName == CommonClassNames.JAVA_LANG_ANNOTATION_ANNOTATION

    override fun setName(name: String): PsiElement =
        throw IncorrectOperationException("Impossible to rename ${JvmAbi.REPEATABLE_ANNOTATION_CONTAINER_NAME}")

    private val _ownMethods: List<KtLightMethod> by lazyPub {
        val lightMethodBuilder = LightMethodBuilder(
            manager, language, "value",
            LightParameterListBuilder(manager, language),
            LightModifierList(manager, language, PsiModifier.PUBLIC, PsiModifier.ABSTRACT)
        )

        lightMethodBuilder.setMethodReturnType {
            val qualifier = containingClass?.qualifiedName ?: return@setMethodReturnType null
            JavaPsiFacade.getElementFactory(project).createTypeByFQClassName(qualifier, resolveScope).createArrayType()
        }

        listOf(
            KtUltraLightMethodForSourceDeclaration(
                delegate = lightMethodBuilder,
                lightMemberOrigin = null,
                support = support,
                containingClass = this,
                forceToSkipNullabilityAnnotation = false,
                methodIndex = METHOD_INDEX_FOR_NON_ORIGIN_METHOD,
            )
        )
    }
}

private class KtUltraLightModifierListForRepeatableAnnotationContainer(
    private val containingClass: KtLightClassForSourceDeclaration,
    support: KtUltraLightSupport,
) : KtUltraLightModifierList<KtLightClassForSourceDeclaration>(containingClass, support) {
    override fun hasModifierProperty(name: String): Boolean = name in modifiers
    override fun copy() = KtUltraLightModifierListForRepeatableAnnotationContainer(containingClass, support)
    override fun PsiAnnotation.additionalConverter(): KtLightAbstractAnnotation? = tryConvertAsRepeatableContainer(support)
    override val annotationsFilter: ((KtLightAbstractAnnotation) -> Boolean) = { it.qualifiedName in allowedAnnotations }

    companion object {
        private val allowedAnnotations = setOf(
            KOTLIN_JVM_INTERNAL_REPEATABLE_CONTAINER,
            CommonClassNames.JAVA_LANG_ANNOTATION_RETENTION,
            StandardNames.FqNames.retention.asString(),
            CommonClassNames.JAVA_LANG_ANNOTATION_TARGET,
            StandardNames.FqNames.target.asString(),
        )

        // It is marked as Abstract because all the annotation classes are marked as Abstract
        // It is marked as Static because all nested interfaces marked as Static
        private val modifiers = setOf(PsiModifier.PUBLIC, PsiModifier.ABSTRACT, PsiModifier.STATIC)
    }
}
