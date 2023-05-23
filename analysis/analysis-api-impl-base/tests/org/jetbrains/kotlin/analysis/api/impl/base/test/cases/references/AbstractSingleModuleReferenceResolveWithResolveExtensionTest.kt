/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.impl.base.test.util.KtSingleModuleResolveExtensionProviderForTest
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KtResolveExtensionFile
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KtResolveExtensionProvider
import org.jetbrains.kotlin.name.FqName

abstract class AbstractSingleModuleReferenceResolveWithResolveExtensionTest : AbstractReferenceResolveWithResolveExtensionTest() {
    override fun createResolveExtensionProvider(
        files: List<KtResolveExtensionFile>,
        packages: Set<FqName>,
        shadowedScope: GlobalSearchScope,
    ): KtResolveExtensionProvider =
        KtSingleModuleResolveExtensionProviderForTest(files, packages, shadowedScope)
}
