/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests

import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion

// TODO: remove after KT-63862
const val IMPL_VERSION_SYSTEM_PROPERTY = "kotlin.build-tools-api.impl-version"
val buildToolsVersion = System.getProperty(IMPL_VERSION_SYSTEM_PROPERTY)
    ?.let { KotlinToolingVersion(it) }
    ?: error("The build tools implementation version must be passed via the `$IMPL_VERSION_SYSTEM_PROPERTY` system property to the tests")
