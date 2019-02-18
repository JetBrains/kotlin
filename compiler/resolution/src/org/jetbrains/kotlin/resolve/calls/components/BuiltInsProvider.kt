/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.NewConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl


interface BuiltInsProvider {
    val builtIns: KotlinBuiltIns
}

internal val ConstraintSystemBuilder.builtIns: KotlinBuiltIns get() = ((this as NewConstraintSystemImpl).typeSystemContext as BuiltInsProvider).builtIns
internal val NewConstraintSystem.builtIns: KotlinBuiltIns get() = ((this as NewConstraintSystemImpl).typeSystemContext as BuiltInsProvider).builtIns