/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.filters

/** A [TestMetadataFilter] of [includePaths] in the form `@TestMetadata` uses. */
internal fun testMetadataFilter(vararg includePaths: String): TestMetadataFilter =
    TestMetadataFilter(includePaths.map(::TestMetadataPath))
