/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references

import org.jetbrains.kotlin.analysis.api.impl.base.test.util.KtResolveExtensionFileForTests
import org.jetbrains.kotlin.analysis.api.impl.base.test.util.KtResolveExtensionProviderForTestPreAnalysisHandler
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KtResolveExtensionFile
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KtResolveExtensionProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.bind
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder

abstract class AbstractReferenceResolveWithResolveExtensionTest : AbstractReferenceResolveTest() {
    abstract fun createResolveExtensionProvider(
        files: List<KtResolveExtensionFile>,
        packages: Set<FqName>,
    ): KtResolveExtensionProvider

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        val provider = createResolveExtensionProvider(
            listOf(
                KtResolveExtensionFileForTests(
                    "extension1.kt",
                    packageName = FqName("generated"),
                    topLevelClassifiersNames = setOf("GeneratedClass1"),
                    topLevelCallableNames = setOf("generatedTopLevelFunction1", "generatedTopLevelExtensionFunction1"),
                    fileText = """|package generated
                       |
                       |class GeneratedClass1 {
                       |   fun generatedClassMember1(): Int
                       |}
                       |
                       |fun generatedTopLevelFunction1(): GeneratedClass2
                       |
                       |fun String.generatedTopLevelExtensionFunction1(boolean: Boolean): Int
                    """.trimMargin()
                ),
                KtResolveExtensionFileForTests(
                    "extension2.kt",
                    packageName = FqName("generated"),
                    topLevelClassifiersNames = setOf("GeneratedClass2"),
                    topLevelCallableNames = setOf(),
                    fileText = """|package generated
                       |
                       |class GeneratedClass2 {
                       |   fun generatedClassMember2(): GeneratedClass1
                       |}
                    """.trimMargin(),
                )
            ),
            setOf(FqName("generated"))
        )
        with(builder) {
            usePreAnalysisHandlers(::KtResolveExtensionProviderForTestPreAnalysisHandler.bind(listOf(provider)))
        }
    }
}
