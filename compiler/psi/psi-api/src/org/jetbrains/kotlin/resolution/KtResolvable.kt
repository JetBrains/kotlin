/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolution

import org.jetbrains.kotlin.psi.KtExperimentalApi

/**
 * A marker interface for all elements that can be resolved into a symbol in the Analysis API.
 *
 * See [References and calls](https://kotlin.github.io/analysis-api/references-and-calls.html) for more details
 */
@KtExperimentalApi
interface KtResolvable

/**
 * A marker interface for all elements that can be resolved into a call in the Analysis API.
 *
 * See [References and calls](https://kotlin.github.io/analysis-api/references-and-calls.html) for more details
 */
@KtExperimentalApi
interface KtResolvableCall : KtResolvable
