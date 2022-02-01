/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.CompositeClassLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext

internal val jvmClassPreprocessingPhase = makeIrFilePhase(
    { context: JvmBackendContext ->
        CompositeClassLoweringPass(
            TypeAliasAnnotationMethodsLowering(context),
            JvmOverloadsAnnotationLowering(context),
            MainMethodGenerationLowering(context),
            AnnotationLowering(),
            JvmDefaultConstructorLowering(context),
            AdditionalClassAnnotationLowering(context)
        )
    },
    name = "JvmClassPreprocessing",
    description = "Generate some JVM-specific class members & annotations"
)