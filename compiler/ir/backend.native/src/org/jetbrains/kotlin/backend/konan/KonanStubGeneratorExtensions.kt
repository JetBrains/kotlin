/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.util.StubGeneratorExtensions

object KonanStubGeneratorExtensions : StubGeneratorExtensions() {
    override fun isPropertyWithPlatformField(descriptor: PropertyDescriptor): Boolean {
        return super.isPropertyWithPlatformField(descriptor) || descriptor.isLateInit
    }
}