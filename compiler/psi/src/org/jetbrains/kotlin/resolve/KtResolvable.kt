/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

/**
 * This is a marker interface for all elements that can be resolved in the Analysis API.
 */
interface KtResolvable

interface KtResolvableCall : KtResolvable