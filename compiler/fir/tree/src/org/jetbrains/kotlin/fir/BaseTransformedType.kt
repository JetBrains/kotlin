/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

// This annotation marks FIR interfaces which may be used as transformer function results
// In case some interface is not marked, transformer function returns closest parent marked with this annotation

annotation class BaseTransformedType