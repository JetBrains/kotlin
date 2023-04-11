/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references

import org.jetbrains.kotlin.analysis.api.impl.base.test.util.KtResolveAbstentionProviderForTest
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.services.PreAnalysisHandler
import org.jetbrains.kotlin.analysis.api.impl.base.test.util.KtResolveAbstentionProviderForTestPreAnalysisHandler
import org.jetbrains.kotlin.analysis.providers.KtResolveExtensionFile
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.bind

abstract class AbstractReferenceResolveWithResolveExtensionTest : AbstractReferenceResolveTest() {
    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        val provider = KtResolveAbstentionProviderForTest(
            listOf(
                KtResolveExtensionFile(
                    """|package generated
                       |
                       |class GeneratedClass1 {
                       |   fun generatedClassMember1(): Int
                       |}
                       |
                       |fun generatedTopLevelFunction1(): GeneratedClass2
                       |
                       |fun String.generatedTopLevelExtensionFunction1(boolean: Boolean): Int
                    """.trimMargin(),
                    "extension1.kt"
                ),
                KtResolveExtensionFile(
                    """|package generated
                       |
                       |class GeneratedClass2 {
                       |   fun generatedClassMember2(): GeneratedClass1
                       |}
                    """.trimMargin(),
                    "extension1.kt"
                )
            ),
            setOf(FqName("generated"))
        )
        with(builder) {
            usePreAnalysisHandlers(::KtResolveAbstentionProviderForTestPreAnalysisHandler.bind(listOf(provider)))
        }
    }
}
