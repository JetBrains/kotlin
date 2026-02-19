/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.mpp

import org.jetbrains.kotlin.name.ClassId

/**
 * Common interface for type references to be used in abstract checker.
 * The idea is similar to [org.jetbrains.kotlin.types.model.KotlinTypeMarker],
 * but type reference, unlike a type, has source element.
 *
 * Used in [org.jetbrains.kotlin.resolve.calls.mpp.AbstractExpectActualAnnotationMatchChecker].
 */
interface TypeRefMarker
