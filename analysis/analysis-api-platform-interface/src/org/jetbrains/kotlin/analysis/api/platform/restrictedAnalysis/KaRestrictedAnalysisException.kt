/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.restrictedAnalysis

/**
 * An exception which occurred during [restricted analysis][KotlinRestrictedAnalysisService].
 *
 * The Analysis API wraps exceptions which occur during restricted analysis in [KaRestrictedAnalysisException]. Due to the incomplete,
 * inconsistent, and potentially changing information available during restricted analysis mode, the Analysis API's resolution and caches
 * might run into inconsistent states. This can trigger exceptions from Analysis API and Kotlin compiler internals. Similar to how Analysis
 * API results might not be as expected during restricted analysis, there might be exceptions which usually couldn't occur with a consistent
 * project state.
 *
 * Distinguishing between regular exceptions and those that occurred during restricted analysis supports diagnostic efforts and even allows
 * the platform to suppress such exceptions if needed.
 */
public abstract class KaRestrictedAnalysisException : Exception()
