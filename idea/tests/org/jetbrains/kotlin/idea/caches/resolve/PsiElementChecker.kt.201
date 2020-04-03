/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.util.Key
import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightModifierList
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.junit.Assert

object PsiElementChecker {
    val TEST_DATA_KEY = Key.create<Int>("Test Key")

    fun checkPsiElementStructure(lightClass: PsiClass) {
        checkPsiElement(lightClass)

        lightClass.methods.forEach {
            it.parameterList.parameters.forEach { checkPsiElement(it) }
            checkPsiElement(it)
        }

        lightClass.fields.forEach { checkPsiElement(it) }

        lightClass.innerClasses.forEach { checkPsiElementStructure(it) }
    }

    private fun checkPsiElement(element: PsiElement) {
        if (element !is KtLightElement<*, *> && element !is KtLightModifierList<*>) return

        if (element is PsiModifierListOwner) {
            val modifierList = element.modifierList
            if (modifierList != null) {
                checkPsiElement(modifierList)
            }
        }

        if (element is PsiTypeParameterListOwner) {
            val typeParameterList = element.typeParameterList
            if (typeParameterList != null) {
                checkPsiElement(typeParameterList)
                typeParameterList.typeParameters.forEach { checkPsiElement(it) }
            }
        }

        with(element) {
            try {
                Assert.assertEquals("Number of methods has changed. Please update test.", 56, PsiElement::class.java.methods.size)

                project
                Assert.assertTrue(language == KotlinLanguage.INSTANCE)
                manager
                children
                parent
                firstChild
                lastChild
                nextSibling
                prevSibling
                containingFile
                textRange
                //textRangeInParent - throws an exception for non-physical elements, it is expected behaviour
                startOffsetInParent
                textLength
                findElementAt(0)
                findReferenceAt(0)
                textOffset
                text
                textToCharArray()
                navigationElement
                originalElement
                textMatches("")
                Assert.assertTrue(textMatches(this))
                textContains('a')
                accept(PsiElementVisitor.EMPTY_VISITOR)
                acceptChildren(PsiElementVisitor.EMPTY_VISITOR)

                val copy = copy()
                Assert.assertTrue(copy == null || copy::class.java == this::class.java)

                // Modify methods:
                // add(this)
                // addBefore(this, lastChild)
                // addAfter(firstChild, this)
                // checkAdd(this)
                // addRange(firstChild, lastChild)
                // addRangeBefore(firstChild, lastChild, lastChild)
                // addRangeAfter(firstChild, lastChild, firstChild)
                // delete()
                // checkDelete()
                // deleteChildRange(firstChild, lastChild)
                // replace(this)

                Assert.assertTrue(isValid)
                isWritable
                @Suppress("UnstableApiUsage") ownReferences
                reference
                references
                putCopyableUserData(TEST_DATA_KEY, 12)

                Assert.assertTrue(getCopyableUserData(TEST_DATA_KEY) == 12)
                // Assert.assertTrue(copy().getCopyableUserData(TEST_DATA_KEY) == 12) { this } Doesn't work

                // processDeclarations(...)

                context
                isPhysical
                resolveScope
                useScope
                node
                toString()
                Assert.assertTrue(isEquivalentTo(this))
            } catch (t: Throwable) {
                throw AssertionErrorWithCause("Failed for ${this::class.java} ${this}", t)
            }
        }
    }
}