/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references

import org.jetbrains.kotlin.analysis.api.impl.base.test.util.KtMultiModuleResolveExtensionProviderForTest
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KtResolveExtensionFile
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KtResolveExtensionProvider
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.name.FqName

abstract class AbstractMultiModuleReferenceResolveWithResolveExtensionTest : AbstractReferenceResolveWithResolveExtensionTest() {
    override fun createResolveExtensionProvider(files: List<KtResolveExtensionFile>, packages: Set<FqName>): KtResolveExtensionProvider =
        KtMultiModuleResolveExtensionProviderForTest(files, packages) { it is KtSourceModule && it.moduleName == "extendedModule" }
}
