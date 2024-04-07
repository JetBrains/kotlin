/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.konan

import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.SimpleDirective
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

interface KnmTestSupport {
    val ignoreDirective: SimpleDirective
    fun createDecompiler(): KlibMetadataDecompiler<*>
}

object Fe10KnmTestSupport : KnmTestSupport {
    private object Directives : SimpleDirectivesContainer() {
        val KNM_FE10_IGNORE by directive(
            description = "Ignore test for KNM files with FE10 K/N Decompiler",
            applicability = DirectiveApplicability.Global,
        )
    }

    override val ignoreDirective: SimpleDirective
        get() = Directives.KNM_FE10_IGNORE

    override fun createDecompiler(): KlibMetadataDecompiler<*> {
        return KotlinNativeMetadataDecompiler()
    }
}

object K2KnmTestSupport : KnmTestSupport {
    private object Directives : SimpleDirectivesContainer() {
        val KNM_K2_IGNORE by directive(
            description = "Ignore test for KNM files with K2 K/N Decompiler",
            applicability = DirectiveApplicability.Global,
        )
    }

    override val ignoreDirective: SimpleDirective
        get() = Directives.KNM_K2_IGNORE

    override fun createDecompiler(): KlibMetadataDecompiler<*> {
        return K2KotlinNativeMetadataDecompiler()
    }
}
