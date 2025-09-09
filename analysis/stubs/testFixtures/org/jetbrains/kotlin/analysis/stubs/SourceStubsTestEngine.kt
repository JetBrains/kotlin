/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.stubs

import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubImpl

object SourceStubsTestEngine : StubsTestEngine() {
    override fun compute(file: KtFile): KotlinFileStubImpl = file.calcStubTree().root as KotlinFileStubImpl
}
