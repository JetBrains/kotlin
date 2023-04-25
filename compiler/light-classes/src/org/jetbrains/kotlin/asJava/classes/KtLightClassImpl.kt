/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.*
import com.intellij.psi.impl.InheritanceImplUtil
import com.intellij.psi.scope.PsiScopeProcessor
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.elements.FakeFileForLightClass
import org.jetbrains.kotlin.asJava.hasInterfaceDefaultImpls
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.resolve.DescriptorUtils

// light class for top level or (inner/nested of top level) source declarations
abstract class KtLightClassImpl(
    classOrObject: KtClassOrObject,
    jvmDefaultMode: JvmDefaultMode,
) : KtLightClassForSourceDeclaration(classOrObject, jvmDefaultMode) {
    fun getDescriptor() =
        LightClassGenerationSupport.getInstance(project).resolveToDescriptor(classOrObject) as? ClassDescriptor

    private val _deprecated by lazyPub { classOrObject.isDeprecated() }

    override fun isDeprecated(): Boolean = _deprecated

    protected open fun computeModifiersByPsi(): Set<String> {
        val psiModifiers = hashSetOf<String>()

        // PUBLIC, PROTECTED, PRIVATE
        //noinspection unchecked

        for (tokenAndModifier in ktTokenToPsiModifier) {
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

        // ABSTRACT
        if (isAbstract() || isSealed()) {
            psiModifiers.add(PsiModifier.ABSTRACT)
        }

        // STATIC
        if (!classOrObject.isTopLevel() && !classOrObject.hasModifier(KtTokens.INNER_KEYWORD)) {
            psiModifiers.add(PsiModifier.STATIC)
        }

        return psiModifiers
    }

    protected open fun computeIsFinal(): Boolean = when {
        classOrObject.hasModifier(KtTokens.FINAL_KEYWORD) -> true
        isAbstract() || isSealed() -> false
        isEnum -> false
        !classOrObject.hasModifier(KtTokens.OPEN_KEYWORD) -> {
            val descriptor = lazy { getDescriptor() }
            var modifier = PsiModifier.FINAL
            project.applyCompilerPlugins {
                modifier = it.interceptModalityBuilding(kotlinOrigin, descriptor, modifier)
            }

            modifier == PsiModifier.FINAL
        }

        else -> false
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

    private val _containingFile: PsiFile by lazyPub {
        val lightClass = if (classOrObject.isTopLevel()) this else getOutermostClassOrObject(classOrObject).toLightClass()!!
        object : FakeFileForLightClass(classOrObject.containingKtFile, lightClass) {
            override fun findReferenceAt(offset: Int) = ktFile.findReferenceAt(offset)

            override fun processDeclarations(
                processor: PsiScopeProcessor,
                state: ResolveState,
                lastParent: PsiElement?,
                place: PsiElement
            ): Boolean {
                if (!super.processDeclarations(processor, state, lastParent, place)) return false

                // We have to explicitly process package declarations if current file belongs to default package
                // so that Java resolve can find classes located in that package
                val packageName = packageName
                if (packageName.isNotEmpty()) return true

                val aPackage = JavaPsiFacade.getInstance(myManager.project).findPackage(packageName)
                if (aPackage != null && !aPackage.processDeclarations(processor, state, null, place)) return false

                return true
            }
        }
    }

    override fun getContainingFile(): PsiFile? = _containingFile

    override fun getOwnInnerClasses(): List<PsiClass> {
        val result = ArrayList<PsiClass>()
        classOrObject.declarations.filterIsInstance<KtClassOrObject>()
            // workaround for ClassInnerStuffCache not supporting classes with null names, see KT-13927
            // inner classes with null names can't be searched for and can't be used from java anyway
            // we can't prohibit creating light classes with null names either since they can contain members
            .filter { it.name != null }
            .mapNotNullTo(result, KtClassOrObject::toLightClass)

        if (classOrObject.hasInterfaceDefaultImpls && jvmDefaultMode != JvmDefaultMode.ALL_INCOMPATIBLE) {
            result.add(createClassForInterfaceDefaultImpls())
        }

        return result
    }

    protected abstract fun createClassForInterfaceDefaultImpls(): PsiClass

    override fun getContainingClass(): PsiClass? {
        if (classOrObject.parent === classOrObject.containingFile) return null

        val containingClassOrObject = (classOrObject.parent as? KtClassBody)?.parent as? KtClassOrObject
        if (containingClassOrObject != null) {
            return containingClassOrObject.toLightClass()
        }

        return null
    }

    companion object {
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

        private val ktTokenToPsiModifier = listOf(
            KtTokens.PUBLIC_KEYWORD to PsiModifier.PUBLIC,
            KtTokens.INTERNAL_KEYWORD to PsiModifier.PUBLIC,
            KtTokens.PROTECTED_KEYWORD to PsiModifier.PROTECTED,
        )
    }
}
