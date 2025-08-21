/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

/**
 * This annotation used for marking low-level services of test infrastructure which
 *   normally should not be used. Please think twice before using something
 *   marked with this annotation
 */
@RequiresOptIn
annotation class TestInfrastructureInternals
