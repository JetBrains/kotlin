/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment
import org.junit.Assert

class KtPsiFactoryTest : KotlinTestWithEnvironment() {
    fun testCreateModifierList() {
        val psiFactory = KtPsiFactory(project)
        KtTokens.MODIFIER_KEYWORDS_ARRAY.forEach {
            val modifier = psiFactory.createModifierList(it)
            Assert.assertTrue(modifier.hasModifier(it))
        }
    }

    fun testEmptyRawStringTemplate() {
        val psiFactory = KtPsiFactory(project)
        val template = psiFactory.createRawStringTemplate("")
        Assert.assertEquals("\"\"\"\"\"\"", template.text)
    }

    fun testSingleLineRawStringTemplate() {
        val psiFactory = KtPsiFactory(project)
        val template = psiFactory.createRawStringTemplate("Foo Bar")
        Assert.assertEquals("\"\"\"Foo Bar\"\"\"", template.text)
    }

    fun testSingleLineRawStringTemplateWithEntries() {
        val psiFactory = KtPsiFactory(project)
        val template = psiFactory.createRawStringTemplate("\$Foo \${Bar}")
        Assert.assertEquals("\"\"\"\$Foo \${Bar}\"\"\"", template.text)
    }

    fun testMultiLineRawStringTemplate() {
        val psiFactory = KtPsiFactory(project)
        val template = psiFactory.createRawStringTemplate("Foo\nBar\nBaz")
        Assert.assertEquals("\"\"\"Foo\nBar\nBaz\"\"\"", template.text)
    }

    fun testMultiLineRawStringTemplateWithEntries() {
        val psiFactory = KtPsiFactory(project)
        val template = psiFactory.createRawStringTemplate("\$Foo\n\${Bar}")
        Assert.assertEquals("\"\"\"\$Foo\n\${Bar}\"\"\"", template.text)
    }

    override fun createEnvironment(): KotlinCoreEnvironment {
        return KotlinCoreEnvironment.createForTests(
            testRootDisposable, KotlinTestUtils.newConfiguration(), EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
    }
}
