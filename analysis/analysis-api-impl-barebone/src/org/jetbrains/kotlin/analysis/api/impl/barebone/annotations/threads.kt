/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.barebone.annotations

@Target(AnnotationTarget.CLASS)
annotation class ThreadSafe

@Target(AnnotationTarget.CLASS)
annotation class NotThreadSafe

@Target(AnnotationTarget.CLASS)
annotation class Immutable