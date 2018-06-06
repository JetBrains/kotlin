/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compatibility

import com.intellij.openapi.Disposable
import com.intellij.testFramework.fixtures.CodeInsightTestFixture

/**
 * Method was introduced in 173 idea. Should be dropped after abandoning 172 branch.
 * BUNCH: 173
 */
@Suppress("IncompatibleAPI")
val CodeInsightTestFixture.projectDisposableEx: Disposable get() = projectDisposable