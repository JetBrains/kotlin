/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json

import org.jetbrains.kotlin.arguments.dsl.types.ProfileCompilerCommand
import org.jetbrains.kotlin.arguments.dsl.types.ProfileCompilerCommandType
import org.jetbrains.kotlin.arguments.serialization.json.base.CustomTypeSerializer

object ProfileCompilerCommandSerializer : CustomTypeSerializer<ProfileCompilerCommand>(
    ProfileCompilerCommandType::class.qualifiedName!!,
    ProfileCompilerCommand::class.qualifiedName!!
)
