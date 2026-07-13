/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import org.jetbrains.kotlin.config.LanguageFeature

/**
 * Declarations that are only being used in case some default language features are disabled: mostly likely due to using not the latest
 * language version.
 *
 * Let's try to have a convention to use a relevant language feature name as a side comment.
 */
@Suppress("unused")
@RequiresOptIn
annotation class OnlyForDefaultLanguageFeatureDisabled(val languageFeature: LanguageFeature, vararg val otherFeatures: LanguageFeature)
