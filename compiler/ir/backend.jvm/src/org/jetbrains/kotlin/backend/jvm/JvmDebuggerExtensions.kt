/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction

interface JvmDebuggerExtensions {
    // Extension point for the JVM Debugger IDEA plug-in: to replace accesses
    // to private properties _without_ accessor implementations, the fragment
    // compiler needs to predict the compilation output for properties.
    // To do this, we need to know whether the property accessors have explicit
    // bodies, information that is _not_ present in the IR structure, but _is_
    // available in the corresponding PSI. See `CodeFragmentCompiler` in the
    // plug-in for the implementation.
    fun isAccessorWithExplicitImplementation(accessor: IrSimpleFunction): Boolean
}
