/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.lower

import org.jetbrains.kotlin.bir.backend.BirLoweringPhase
import org.jetbrains.kotlin.bir.backend.jvm.JvmBirBackendContext
import org.jetbrains.kotlin.bir.declarations.BirClass
import org.jetbrains.kotlin.bir.declarations.BirConstructor
import org.jetbrains.kotlin.bir.declarations.BirModuleFragment
import org.jetbrains.kotlin.bir.remove
import org.jetbrains.kotlin.descriptors.ClassKind

context(JvmBirBackendContext)
class BirAnnotationLowering : BirLoweringPhase() {
    override fun lower(module: BirModuleFragment) {
        getAllElementsOfClass(BirClass, false).forEach { clazz ->
            if (clazz.kind == ClassKind.ANNOTATION_CLASS) {
                clazz.declarations
                    .filterIsInstance<BirConstructor>()
                    .forEach { it.remove() }
            }
        }
    }
}