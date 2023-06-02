/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.mpp

/**
 * The marker interface marks all Java descriptors that can implicitly actualize expect declarations.
 *
 * The marker interface is needed for being able to access information about Java specific descriptors from platform-agnostic
 * `:compiler:frontend` module
 */
interface MppJavaImplicitActualizatorMarker
