/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analyzer

import com.intellij.util.messages.Topic
import org.jetbrains.kotlin.descriptors.ModuleDescriptor

interface ModuleDescriptorListener {
    fun moduleDescriptorInvalidated(moduleDescriptor: ModuleDescriptor)

    companion object {
        @JvmField
        val TOPIC: Topic<ModuleDescriptorListener> =
            Topic.create("ModuleDescriptorListener", ModuleDescriptorListener::class.java)
    }
}