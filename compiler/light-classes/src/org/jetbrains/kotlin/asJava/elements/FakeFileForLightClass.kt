/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import com.intellij.psi.impl.source.PsiFileImpl
import com.intellij.psi.impl.source.SourceTreeToPsiMap
import com.intellij.psi.impl.source.tree.TreeElement
import com.intellij.psi.stubs.PsiClassHolderFileStub
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
    private val lightClass: () -> KtLightClass,
    private val stub: () -> PsiClassHolderFileStub<*>,
    private val packageFqName: FqName = ktFile.packageFqName
) : ClsFileImpl(ktFile.viewProvider) {

    override fun getVirtualFile(): VirtualFile =
        ktFile.virtualFile ?: ktFile.originalFile.virtualFile ?: super.getVirtualFile()

    override fun getPackageName() = packageFqName.asString()

    override fun getStub() = stub()

    override fun getClasses() = arrayOf(lightClass())

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
    override fun getLanguageLevel() = LanguageLevel.JDK_1_6

    override fun hashCode(): Int {
        val thisClass = lightClass()
        if (thisClass is KtLightClassForSourceDeclaration) return ktFile.hashCode()
        return thisClass.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FakeFileForLightClass) return false
        val thisClass = lightClass()
        val anotherClass = other.lightClass()

        if (thisClass is KtLightClassForSourceDeclaration) {
            return anotherClass is KtLightClassForSourceDeclaration && ktFile == other.ktFile
        }

        return thisClass == anotherClass
    }

    override fun isEquivalentTo(another: PsiElement?) = this == another

    override fun setPackageName(packageName: String) {
        if (lightClass() is KtLightClassForFacade) {
            ktFile.packageDirective?.fqName = FqName(packageName)
        } else {
            super.setPackageName(packageName)
        }
    }

    override fun isPhysical() = false
}
