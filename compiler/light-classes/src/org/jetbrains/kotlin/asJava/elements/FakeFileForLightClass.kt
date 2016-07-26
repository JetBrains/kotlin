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

import com.intellij.pom.java.LanguageLevel
import com.intellij.psi.ClassFileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.impl.compiled.ClsFileImpl
import com.intellij.psi.stubs.PsiClassHolderFileStub
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile

open class FakeFileForLightClass(
        protected val ktFile: KtFile,
        private val lightClass: () -> KtLightClass,
        private val stub: () -> PsiClassHolderFileStub<*>,
        private val packageFqName: FqName = ktFile.packageFqName
) : ClsFileImpl(ClassFileViewProvider(ktFile.manager, ktFile.virtualFile)) {
    override fun getPackageName() = packageFqName.asString()

    override fun getStub() = stub()

    override fun getClasses() = arrayOf(lightClass())

    override fun getNavigationElement() = ktFile

    override fun accept(visitor: PsiElementVisitor) {
        // Prevent access to compiled PSI
        // TODO: More complex traversal logic may be implemented when necessary
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
}