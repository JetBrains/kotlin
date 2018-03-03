/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.js.translate.context.generator

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

class Generator<out V : Any>(vararg val rules: (DeclarationDescriptor) -> V?) {

    private val cache = mutableMapOf<DeclarationDescriptor, V?>()

    operator fun get(descriptor: DeclarationDescriptor): V? {
        return cache.getOrPut(descriptor) {
            rules.firstNotNullResult {
                it(descriptor)
            }
        }
    }
}
