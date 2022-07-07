/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifier
import com.intellij.psi.impl.InheritanceImplUtil
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.resolve.DescriptorUtils

// light class for top level or (inner/nested of top level) source declarations
abstract class KtLightClassImpl @JvmOverloads constructor(
    classOrObject: KtClassOrObject,
    jvmDefaultMode: JvmDefaultMode,
    forceUsingOldLightClasses: Boolean = false
) : KtLightClassForSourceDeclaration(classOrObject, jvmDefaultMode, forceUsingOldLightClasses) {
    fun getDescriptor() =
        LightClassGenerationSupport.getInstance(project).resolveToDescriptor(classOrObject) as? ClassDescriptor

    override fun computeModifiers(): Set<String> {
        val psiModifiers = hashSetOf<String>()

        // PUBLIC, PROTECTED, PRIVATE, ABSTRACT, FINAL
        //noinspection unchecked

        for (tokenAndModifier in jetTokenToPsiModifier) {
            if (classOrObject.hasModifier(tokenAndModifier.first)) {
                psiModifiers.add(tokenAndModifier.second)
            }
        }

        if (classOrObject.hasModifier(KtTokens.PRIVATE_KEYWORD)) {
            // Top-level private class has PACKAGE_LOCAL visibility in Java
            // Nested private class has PRIVATE visibility
            psiModifiers.add(if (classOrObject.isTopLevel()) PsiModifier.PACKAGE_LOCAL else PsiModifier.PRIVATE)
        } else if (!psiModifiers.contains(PsiModifier.PROTECTED)) {
            psiModifiers.add(PsiModifier.PUBLIC)
        }

        // ABSTRACT | FINAL
        when {
            isAbstract() || isSealed() -> {
                psiModifiers.add(PsiModifier.ABSTRACT)
            }

            isEnum -> {
                // Enum class should not be `final`, since its enum entries extend it.
                // It could be either `abstract` w/o ctor, or empty modality w/ private ctor.
            }

            !(classOrObject.hasModifier(KtTokens.OPEN_KEYWORD)) -> {
                val descriptor = lazy { getDescriptor() }
                var modifier = PsiModifier.FINAL
                project.applyCompilerPlugins {
                    modifier = it.interceptModalityBuilding(kotlinOrigin, descriptor, modifier)
                }
                if (modifier == PsiModifier.FINAL) {
                    psiModifiers.add(PsiModifier.FINAL)
                }
            }
        }

        if (!classOrObject.isTopLevel() && !classOrObject.hasModifier(KtTokens.INNER_KEYWORD)) {
            psiModifiers.add(PsiModifier.STATIC)
        }

        return psiModifiers
    }

    private fun isAbstract(): Boolean = classOrObject.hasModifier(KtTokens.ABSTRACT_KEYWORD) || isInterface

    private fun isSealed(): Boolean = classOrObject.hasModifier(KtTokens.SEALED_KEYWORD)

    override fun isInheritor(baseClass: PsiClass, checkDeep: Boolean): Boolean {
        if (manager.areElementsEquivalent(baseClass, this)) return false
        LightClassInheritanceHelper.getService(project).isInheritor(this, baseClass, checkDeep).ifSure { return it }

        val qualifiedName: String? = if (baseClass is KtLightClassImpl) {
            baseClass.getDescriptor()?.let(DescriptorUtils::getFqName)?.asString()
        } else {
            baseClass.qualifiedName
        }

        val thisDescriptor = getDescriptor()

        return if (qualifiedName != null && thisDescriptor != null) {
            qualifiedName != DescriptorUtils.getFqName(thisDescriptor).asString() &&
                    checkSuperTypeByFQName(thisDescriptor, qualifiedName, checkDeep)
        } else {
            InheritanceImplUtil.isInheritor(this, baseClass, checkDeep)
        }
    }

    override fun getQualifiedName() = classOrObject.fqName?.asString()

    override fun getParent() = if (classOrObject.isTopLevel())
        containingFile
    else
        containingClass

    abstract override fun copy(): PsiElement
}

private fun checkSuperTypeByFQName(classDescriptor: ClassDescriptor, qualifiedName: String, deep: Boolean): Boolean {
    if (CommonClassNames.JAVA_LANG_OBJECT == qualifiedName) return true

    if (qualifiedName == DescriptorUtils.getFqName(classDescriptor).asString()) return true

    val fqName = FqNameUnsafe(qualifiedName)
    val mappedQName =
        if (fqName.isSafe)
            JavaToKotlinClassMap.mapJavaToKotlin(fqName.toSafe())?.asSingleFqName()?.asString()
        else null
    if (qualifiedName == mappedQName) return true

    for (superType in classDescriptor.typeConstructor.supertypes) {
        val superDescriptor = superType.constructor.declarationDescriptor

        if (superDescriptor is ClassDescriptor) {
            val superQName = DescriptorUtils.getFqName(superDescriptor).asString()
            if (superQName == qualifiedName || superQName == mappedQName) return true

            if (deep) {
                if (checkSuperTypeByFQName(superDescriptor, qualifiedName, true)) {
                    return true
                }
            }
        }
    }

    return false
}

private val jetTokenToPsiModifier = listOf(
    KtTokens.PUBLIC_KEYWORD to PsiModifier.PUBLIC,
    KtTokens.INTERNAL_KEYWORD to PsiModifier.PUBLIC,
    KtTokens.PROTECTED_KEYWORD to PsiModifier.PROTECTED,
    KtTokens.FINAL_KEYWORD to PsiModifier.FINAL,
)
