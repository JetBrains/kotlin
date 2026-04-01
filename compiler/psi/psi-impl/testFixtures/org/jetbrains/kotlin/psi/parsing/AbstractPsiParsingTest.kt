/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.parsing

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.impl.DebugUtil
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.test.framework.utils.ignoreExceptionIfIgnoreDirectivePresent
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure
import java.lang.reflect.Modifier

/**
 * Base class for PSI parsing tests that validates PSI tree output against golden `.txt` files.
 *
 * Subclasses provide a [parseKtFile] factory to control how the PSI is created
 * (regular file, expression code fragment, block code fragment).
 */
abstract class AbstractPsiParsingTestBase : AbstractPsiBasedTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + Directives

    private object Directives : SimpleDirectivesContainer() {
        val IGNORE_ERRORS_FROM_API by stringDirective("Ignore errors from the PSI API until they are fixed")
    }

    override fun doTest(file: KtFile, testServices: TestServices) {
        val actual = DebugUtil.psiToString(file, /* showWhitespaces = */ true, /* showRanges = */ false).trim()
        assertEqualsToTestOutputFile(actual)

        testServices.moduleStructure.allDirectives.ignoreExceptionIfIgnoreDirectivePresent(Directives.IGNORE_ERRORS_FROM_API) {
            file.accept(object : KtTreeVisitorVoid() {
                override fun visitKtElement(element: KtElement) {
                    checkPsiGetters(testServices, element)
                    super.visitKtElement(element)
                }
            })
        }
    }

    private fun checkPsiGetters(testServices: TestServices, element: KtElement) {
        for (method in element.javaClass.declaredMethods) {
            val name = method.name
            if (!name.startsWith("get") && !name.startsWith("find")) continue
            if (name == "getReference" || name == "getReferences" || name == "getUseScope" || name == "getPresentation") continue
            if (!Modifier.isPublic(method.modifiers)) continue
            if (method.parameterCount > 0) continue

            val declaringClass = method.declaringClass
            if (!declaringClass.name.startsWith("org.jetbrains.kotlin")) continue

            val result = method.invoke(element)
            if (result == null) {
                for (annotation in method.declaredAnnotations) {
                    if (annotation is IfNotParsed) {
                        testServices.assertions.assertNotNull(PsiTreeUtil.findChildOfType(element, PsiErrorElement::class.java)) {
                            "Incomplete operation in parsed OK test, method $name in ${declaringClass.simpleName} returns null. Element text: \n${element.text}"
                        }
                    }
                }
            }
        }
    }
}

abstract class AbstractPsiParsingTest : AbstractPsiParsingTestBase()

abstract class AbstractExpressionCodeFragmentParsingTest : AbstractPsiParsingTestBase() {
    override fun parseKtFile(
        factory: KtPsiFactory,
        fileName: String,
        content: String,
    ): KtFile = factory.createExpressionCodeFragment(content, null)
}

abstract class AbstractBlockCodeFragmentParsingTest : AbstractPsiParsingTestBase() {
    override fun parseKtFile(
        factory: KtPsiFactory,
        fileName: String,
        content: String,
    ): KtFile = factory.createBlockCodeFragment(content, null)
}
