/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.impl.PsiClassImplUtil
import com.intellij.psi.impl.light.AbstractLightClass
import com.intellij.psi.impl.light.LightMethod
import com.intellij.psi.search.SearchScope
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.toFakeLightClass
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.resolve.DescriptorUtils
import javax.swing.Icon

// Used as a placeholder when actual light class does not exist (expect-classes, for example)
// The main purpose is to allow search of inheritors within hierarchies containing such classes
abstract class KtFakeLightClass(override val kotlinOrigin: KtClassOrObject) :
    AbstractLightClass(kotlinOrigin.manager, KotlinLanguage.INSTANCE),
    KtLightClass {

    private val _delegate: PsiClass by lazy { DummyJavaPsiFactory.createDummyClass(kotlinOrigin.project) }

    override val clsDelegate get() = _delegate
    override val originKind get() = LightClassOriginKind.SOURCE

    override fun getName(): String? = kotlinOrigin.name
    override fun getDelegate(): PsiClass = _delegate
    abstract override fun copy(): KtFakeLightClass

    override fun getQualifiedName(): String? = kotlinOrigin.fqName?.asString()
    abstract override fun getContainingClass(): KtFakeLightClass?
    override fun getNavigationElement(): PsiElement = kotlinOrigin.navigationElement
    override fun getIcon(flags: Int): Icon? = kotlinOrigin.getIcon(flags)
    override fun getContainingFile(): PsiFile = kotlinOrigin.containingFile
    override fun getUseScope(): SearchScope = kotlinOrigin.useScope

    abstract override fun isInheritor(baseClass: PsiClass, checkDeep: Boolean): Boolean

    override fun isEquivalentTo(another: PsiElement?): Boolean = PsiClassImplUtil.isClassEquivalentTo(this, another)
}

class KtDescriptorBasedFakeLightClass(kotlinOrigin: KtClassOrObject) : KtFakeLightClass(kotlinOrigin) {

    override fun copy(): KtFakeLightClass = KtDescriptorBasedFakeLightClass(kotlinOrigin)

    private val _containingClass: KtFakeLightClass? by lazy {
        kotlinOrigin.containingClassOrObject?.let { KtDescriptorBasedFakeLightClass(it) }
    }
    override fun getContainingClass(): KtFakeLightClass? = _containingClass

    override fun isInheritor(baseClass: PsiClass, checkDeep: Boolean): Boolean {
        if (manager.areElementsEquivalent(baseClass, this)) return false
        LightClassInheritanceHelper.getService(project).isInheritor(this, baseClass, checkDeep).ifSure { return it }

        val baseKtClass = (baseClass as? KtLightClass)?.kotlinOrigin ?: return false

        val generationSupport = LightClassGenerationSupport.getInstance(project)

        val baseDescriptor = generationSupport.resolveToDescriptor(baseKtClass) as? ClassDescriptor ?: return false
        val thisDescriptor = generationSupport.resolveToDescriptor(kotlinOrigin) as? ClassDescriptor ?: return false

        val thisFqName = DescriptorUtils.getFqName(thisDescriptor).asString()
        val baseFqName = DescriptorUtils.getFqName(baseDescriptor).asString()
        if (thisFqName == baseFqName) return false

        return if (checkDeep)
            DescriptorUtils.isSubclass(thisDescriptor, baseDescriptor)
        else
            DescriptorUtils.isDirectSubclass(thisDescriptor, baseDescriptor)
    }
}

class KtFakeLightMethod private constructor(
    val ktDeclaration: KtNamedDeclaration,
    ktClassOrObject: KtClassOrObject
) : LightMethod(
    ktDeclaration.manager,
    DummyJavaPsiFactory.createDummyVoidMethod(ktDeclaration.project),
    ktClassOrObject.toFakeLightClass(),
    KotlinLanguage.INSTANCE
), KtLightElement<KtNamedDeclaration, PsiMethod> {
    override val kotlinOrigin get() = ktDeclaration
    override val clsDelegate get() = myMethod

    override fun getName() = ktDeclaration.name ?: ""

    override fun getNavigationElement() = ktDeclaration
    override fun getIcon(flags: Int): Icon? = ktDeclaration.getIcon(flags)
    override fun getUseScope() = ktDeclaration.useScope

    companion object {
        fun get(ktDeclaration: KtNamedDeclaration): KtFakeLightMethod? {
            val ktClassOrObject = ktDeclaration.containingClassOrObject ?: return null
            return KtFakeLightMethod(ktDeclaration, ktClassOrObject)
        }
    }
}

private object DummyJavaPsiFactory {
    fun createDummyVoidMethod(project: Project): PsiMethod {
        // Can't use PsiElementFactory.createMethod() because of formatting in PsiElementFactoryImpl.
        val name = "dummy"
        val returnType = PsiType.VOID

        val canonicalText = GenericsUtil.getVariableTypeByExpressionType(returnType).getCanonicalText(true)
        val file = createDummyJavaFile(project, "class _Dummy_ { public $canonicalText $name() {\n} }")
        val klass = file.classes.singleOrNull()
            ?: throw IncorrectOperationException("Class was not created. Method name: $name; return type: $canonicalText")

        return klass.methods.singleOrNull()
            ?: throw IncorrectOperationException("Method was not created. Method name: $name; return type: $canonicalText")
    }

    fun createDummyClass(project: Project): PsiClass = PsiElementFactory.getInstance(project).createClass("dummy")

    private fun createDummyJavaFile(project: Project, text: String): PsiJavaFile {
        return PsiFileFactory.getInstance(project).createFileFromText(
            DUMMY_FILE_NAME,
            JavaFileType.INSTANCE,
            text
        ) as PsiJavaFile
    }

    private val DUMMY_FILE_NAME = "_Dummy_." + JavaFileType.INSTANCE.defaultExtension
}
