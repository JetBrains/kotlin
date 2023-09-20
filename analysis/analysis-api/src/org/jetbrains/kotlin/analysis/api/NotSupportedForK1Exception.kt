/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api


/**
 * Exception indicating that a certain operation is not supported for the K1 version of Analysis API
 */
public class NotSupportedForK1Exception : UnsupportedOperationException()