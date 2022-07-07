/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.impl.PsiClassImplUtil
import com.intellij.psi.impl.light.AbstractLightClass
import com.intellij.psi.search.SearchScope
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.psi.KtClassOrObject
import javax.swing.Icon

// Used as a placeholder when actual light class does not exist (expect-classes, for example)
// The main purpose is to allow search of inheritors within hierarchies containing such classes
abstract class KtFakeLightClass(override val kotlinOrigin: KtClassOrObject) :
    AbstractLightClass(kotlinOrigin.manager, KotlinLanguage.INSTANCE),
    KtLightClass {

    private val _delegate: PsiClass by lazy { DummyJavaPsiFactory.createDummyClass(kotlinOrigin.project) }

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

object DummyJavaPsiFactory {
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
