/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs

import com.intellij.psi.StubBuilder
import com.intellij.psi.stubs.LanguageStubDefinition
import org.jetbrains.kotlin.psi.stubs.elements.KtFileStubBuilder

/**
 * Kotlin builds stubs from the full PSI/AST (see [KtFileStubBuilder]), not from a light tree, so this
 * implements [LanguageStubDefinition] rather than [com.intellij.psi.stubs.LightLanguageStubDefinition].
 */
internal class KotlinLanguageStubDefinition : LanguageStubDefinition {
    override val stubVersion: Int
        get() = KotlinStubVersions.SOURCE_STUB_VERSION

    override val builder: StubBuilder
        get() = KtFileStubBuilder()
}
