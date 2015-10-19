/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring.pullUp

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.lang.StdLanguages
import com.intellij.psi.*
import com.intellij.refactoring.memberPullUp.JavaPullUpHelper
import com.intellij.refactoring.memberPullUp.PullUpData
import com.intellij.refactoring.memberPullUp.PullUpHelper
import com.intellij.refactoring.memberPullUp.PullUpHelperFactory
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.core.refactoring.createJavaClass
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.startOffset

public class KotlinPullUpHelperFactory : PullUpHelperFactory {
    private fun PullUpData.toKotlinPullUpData(): KotlinPullUpData? {
        val sourceClass = sourceClass.unwrapped as? KtClassOrObject ?: return null
        val targetClass = targetClass.unwrapped as? PsiNamedElement ?: return null
        val membersToMove = membersToMove
                .map { it.namedUnwrappedElement as? KtNamedDeclaration }
                .filterNotNull()
                .sortedBy { it.startOffset }
        return KotlinPullUpData(sourceClass, targetClass, membersToMove)
    }

    override fun createPullUpHelper(data: PullUpData): PullUpHelper<*> {
        if (!data.sourceClass.isInheritor(data.targetClass, true)) return EmptyPullUpHelper
        data.toKotlinPullUpData()?.let { return KotlinPullUpHelper(data, it) }

        if (data.targetClass.language.`is`(KotlinLanguage.INSTANCE) && data.sourceClass.language.`is`(StdLanguages.JAVA)) {
            return JavaToKotlinPostconversionPullUpHelper(data)
        }

        return EmptyPullUpHelper
    }
}

public class JavaToKotlinPullUpHelperFactory : PullUpHelperFactory {
    private fun createJavaToKotlinPullUpHelper(data: PullUpData): JavaToKotlinPreconversionPullUpHelper? {
        if (!data.sourceClass.isInheritor(data.targetClass, true)) return null
        val dummyTargetClass = createDummyTargetClass(data) ?: return null
        val dataForDelegate = object : PullUpData by data {
            override fun getTargetClass() = dummyTargetClass
        }
        return JavaToKotlinPreconversionPullUpHelper(data, dummyTargetClass, JavaPullUpHelper(dataForDelegate))
    }

    private fun createDummyTargetClass(data: PullUpData): PsiClass? {
        val targetClass = data.targetClass.unwrapped as? KtClass ?: return null

        val project = targetClass.project
        val targetPackage = targetClass.getContainingJetFile().packageFqName.asString()
        val dummyFile = PsiFileFactory.getInstance(project).createFileFromText(
                "dummy.java",
                JavaFileType.INSTANCE,
                if (targetPackage.isNotEmpty()) "package $targetPackage;\n" else ""
        )
        val elementFactory = PsiElementFactory.SERVICE.getInstance(project)

        val dummyTargetClass = createJavaClass(targetClass, null, forcePlainClass = true)
        val outerClasses = targetClass.parents.filterIsInstance<KtClassOrObject>().toList().asReversed()

        if (outerClasses.isEmpty()) return dummyFile.add(dummyTargetClass) as PsiClass

        val outerPsiClasses = outerClasses.map {
            val psiClass = elementFactory.createClass(it.name!!)
            if (!(it is KtClass && it.isInner())) {
                psiClass.modifierList!!.setModifierProperty(PsiModifier.STATIC, true)
            }
            psiClass
        }
        return outerPsiClasses
                .drop(1)
                .plus(dummyTargetClass)
                .fold(dummyFile.add(outerPsiClasses.first())) { parent, child -> parent.add(child) } as PsiClass
    }

    override fun createPullUpHelper(data: PullUpData): PullUpHelper<*> {
        createJavaToKotlinPullUpHelper(data)?.let { return it }

        return PullUpHelper.INSTANCE
                       .allForLanguage(StdLanguages.JAVA)
                       .firstOrNull { it !is JavaToKotlinPullUpHelperFactory }
                       ?.createPullUpHelper(data)
               ?: EmptyPullUpHelper
    }
}