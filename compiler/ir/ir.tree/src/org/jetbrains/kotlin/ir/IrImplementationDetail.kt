/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

import org.jetbrains.kotlin.ir.declarations.IrFactory

/**
 * IR implementation classes annotated with this annotation are not expected to be used directly.
 *
 * [IrFactory] should be the preferred way to create instances of such classes.
 */
@RequiresOptIn("Use IrFactory instead of creating IR nodes directly", level = RequiresOptIn.Level.ERROR)
@Target(AnnotationTarget.CONSTRUCTOR)
annotation class IrImplementationDetail
