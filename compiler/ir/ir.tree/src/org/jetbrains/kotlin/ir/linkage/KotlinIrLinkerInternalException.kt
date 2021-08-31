/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.linkage

// Used to terminate linking process. Detailed linkage errors are reported separately to IrMessageLogger.
class KotlinIrLinkerInternalException : Exception("Kotlin IR Linker exception")
