/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.config.LanguageFeature

/**
 * Instructs the annotated argument to enable the specified [org.jetbrains.kotlin.config.LanguageFeature] when set to `true`.
 */
@Target(AnnotationTarget.FIELD)
@Repeatable
annotation class Enables(val feature: LanguageFeature, val ifValueIs: String = "")
