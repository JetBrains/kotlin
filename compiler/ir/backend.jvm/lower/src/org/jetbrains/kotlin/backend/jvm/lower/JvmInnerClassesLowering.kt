/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower;

import org.jetbrains.kotlin.backend.common.lower.InnerClassConstructorCallsLowering
import org.jetbrains.kotlin.backend.common.lower.InnerClassesLowering
import org.jetbrains.kotlin.backend.common.lower.InnerClassesMemberBodyLowering
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext

@PhaseDescription(
    name = "InnerClasses",
    description = "Add 'outer this' fields to inner classes",
    prerequisite = [JvmLocalDeclarationsLowering::class]
)
internal class JvmInnerClassesLowering(context: JvmBackendContext) : InnerClassesLowering(context)

@PhaseDescription(
    name = "InnerClassesMemberBody",
    description = "Replace `this` with 'outer this' field references",
    prerequisite = [JvmInnerClassesLowering::class]
)
internal class JvmInnerClassesMemberBodyLowering(context: JvmBackendContext) : InnerClassesMemberBodyLowering(context)

@PhaseDescription(
    name = "InnerClassConstructorCalls",
    description = "Handle constructor calls for inner classes"
)
internal class JvmInnerClassConstructorCallsLowering(context: JvmBackendContext) : InnerClassConstructorCallsLowering(context)
