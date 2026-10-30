/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.load.java.ValueClassParameterNames
import org.jetbrains.kotlin.name.Name

/**
 * Builds the name of a parameter which holds the underlying value of the inline class [bound].
 * The format and its contract are documented on [ValueClassParameterNames].
 */
fun Name.withValueClassParameterName(bound: IrClass): Name =
    Name.identifier(ValueClassParameterNames.encode(bound.fqNameWhenAvailable?.asString().orEmpty(), asString()))
