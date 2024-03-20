/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.components

import org.jetbrains.kotlin.metadata.jvm.serialization.JvmStringTable
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.protobuf.MessageLite

interface SerializationAwareModuleJavaClassesTracker : ModuleJavaClassesTracker {

    fun serializeJavaClasses(serializer: (ClassId) ->  Pair<MessageLite, JvmStringTable>?)
}
