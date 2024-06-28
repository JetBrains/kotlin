/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.test.testFramework.KtParsingTestCase
import org.jetbrains.kotlin.test.util.KtTestUtil

class GetElementWithContextTest : KtParsingTestCase(
    "",
    "kt",
    KotlinParserDefinition()
) {

    override fun getTestDataPath() = KtTestUtil.getHomeDirectory()

    fun testGetElementWithContext() {
        val file = KtPsiFactory(myProject).createFile(
            """ |
                |fun foo() {
                |   println(1 + 1) 
                |}
            """.trimMargin()
        )
        val statement =
            (file.declarations.first() as KtNamedFunction).bodyBlockExpression!!.statements.first()
        val text = getElementTextWithContext(statement)
        assertEquals(
            """|<File name: dummy.kt, Physical: false>
               |fun foo() {
               |   <ELEMENT>println(1 + 1)</ELEMENT> 
               |}
        """.trimMargin(), text)
    }
}