/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.platform

/**
 * This class exists only to be able to reference it in modules which does not depend
 * on `:compiler:cli-common`. The only allowed inheritor of this class
 * is [org.jetbrains.kotlin.platform.konan.NativePlatform]
 */
abstract class NativePlatform(platformName: String) : SimplePlatform(platformName)
