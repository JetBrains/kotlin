/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.elements

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.java.LanguageLevel
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.compiled.ClsFileImpl
import com.intellij.psi.impl.java.stubs.ClsStubPsiFactory
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub
import com.intellij.psi.impl.java.stubs.impl.PsiJavaFileStubImpl
import com.intellij.psi.impl.source.PsiFileImpl
import com.intellij.psi.impl.source.SourceTreeToPsiMap
import com.intellij.psi.impl.source.tree.TreeElement
import com.intellij.psi.util.PsiUtil
import com.intellij.reference.SoftReference
import com.intellij.util.AstLoadingFilter
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import java.lang.ref.Reference

open class FakeFileForLightClass(
    val ktFile: KtFile,
    private val lightClass: KtLightClass,
    private val packageFqName: FqName = ktFile.packageFqName,
) : ClsFileImpl(ktFile.viewProvider) {
    @Deprecated("A light class should be provided directly")
    constructor(
        ktFile: KtFile,
        lightClass: () -> KtLightClass,
        packageFqName: FqName = ktFile.packageFqName,
    ) : this(
        ktFile = ktFile,
        lightClass = lightClass(),
        packageFqName = packageFqName,
    )

    override fun getVirtualFile(): VirtualFile =
        ktFile.virtualFile ?: ktFile.originalFile.virtualFile ?: super.getVirtualFile()

    override fun getPackageName() = packageFqName.asString()

    private fun createFakeJavaFileStub(): PsiJavaFileStub {
        val javaFileStub = PsiJavaFileStubImpl(packageFqName.asString(), /* compiled = */true)
        javaFileStub.psiFactory = ClsStubPsiFactory.INSTANCE
        javaFileStub.psi = this
        return javaFileStub
    }

    override fun getStub() = createFakeJavaFileStub()

    override fun getClasses() = arrayOf(lightClass)

    override fun getNavigationElement() = ktFile

    override fun accept(visitor: PsiElementVisitor) {
        // Prevent access to compiled PSI
        // TODO: More complex traversal logic may be implemented when necessary
    }

    @Volatile
    private var myMirrorFileElement: Reference<TreeElement>? = null
    private val myMirrorLock: Any = Any()

    override fun getMirror(): PsiElement {
        SoftReference.dereference(myMirrorFileElement)?.let { return it.psi }

        val mirrorElement = synchronized(myMirrorLock) {
            SoftReference.dereference(myMirrorFileElement)?.let { return@synchronized it }

            val file = this.virtualFile
            AstLoadingFilter.assertTreeLoadingAllowed(file)
            val classes: Array<KtLightClass> = this.classes
            val fileName = (if (classes.isNotEmpty()) classes[0].name else file.nameWithoutExtension) + ".java"
            val document = FileDocumentManager.getInstance().getDocument(file) ?: error(file.url)

            val factory = PsiFileFactory.getInstance(this.manager.project)
            val mirror = factory.createFileFromText(fileName, JavaLanguage.INSTANCE, document.immutableCharSequence, false, false, true)
            mirror.putUserData(PsiUtil.FILE_LANGUAGE_LEVEL_KEY, this.languageLevel)
            val mirrorTreeElement = SourceTreeToPsiMap.psiToTreeNotNull(mirror)
            if (mirror is PsiFileImpl) {
                mirror.originalFile = this
            }
            mirrorTreeElement.also {
                myMirrorFileElement = SoftReference(it)
            }
        }

        return mirrorElement.psi
    }

    // this should be equal to current compiler target language level
    override fun getLanguageLevel() = LanguageLevel.JDK_1_8

    override fun hashCode(): Int = if (lightClass is KtLightClassForSourceDeclaration) ktFile.hashCode() else lightClass.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FakeFileForLightClass) return false

        val anotherClass = other.lightClass
        if (lightClass is KtLightClassForSourceDeclaration) {
            return anotherClass is KtLightClassForSourceDeclaration && ktFile == other.ktFile
        }

        return lightClass == anotherClass
    }

    override fun isEquivalentTo(another: PsiElement?) = this == another

    override fun setPackageName(packageName: String) {
        if (lightClass is KtLightClassForFacade) {
            ktFile.packageDirective?.fqName = FqName(packageName)
        } else {
            super.setPackageName(packageName)
        }
    }

    override fun isPhysical() = false
}
