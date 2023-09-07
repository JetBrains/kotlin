/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.mpp

/**
 * Common interface, which stores a link to a source element of declaration or type reference (or null, if no source).
 * The idea is similar to [DeclarationSymbolMarker], but it doesn't fit, because
 * sources of type references also must be supported, which are not declarations.
 * Used in [org.jetbrains.kotlin.resolve.calls.mpp.AbstractExpectActualAnnotationMatchChecker].
 */
interface SourceElementMarker
