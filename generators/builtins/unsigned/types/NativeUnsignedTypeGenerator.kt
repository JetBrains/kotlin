/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.builtins.unsigned

import org.jetbrains.kotlin.generators.builtins.UnsignedType
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.ExpectActualModifier
import unsigned.types.BaseUnsignedTypeGenerator
import java.io.PrintWriter

class NativeUnsignedTypeGenerator(type: UnsignedType, out: PrintWriter) : BaseUnsignedTypeGenerator(type, out, ExpectActualModifier.Actual)
