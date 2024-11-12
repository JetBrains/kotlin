/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

/**
 * An API that we would like to migrate away from, and eventually drop.
 *
 * Note: We don't simply use @Deprecated annotation because of how our build
 * and infrastructure are configured.
 */
annotation class DeprecatedCompilerApi
