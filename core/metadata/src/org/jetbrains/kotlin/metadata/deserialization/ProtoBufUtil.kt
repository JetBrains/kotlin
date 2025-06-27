/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.metadata.deserialization

import org.jetbrains.kotlin.protobuf.GeneratedMessage

fun <M : GeneratedMessage.ExtendableMessage<M>, T> GeneratedMessage.ExtendableMessage<M>.getExtensionOrNull(
    extension: GeneratedMessage.GeneratedExtension<M, T>
): T? = if (hasExtension(extension)) getExtension(extension) else null

fun <M : GeneratedMessage.ExtendableMessage<M>, T> GeneratedMessage.ExtendableMessage<M>.getExtensionOrNull(
    extension: GeneratedMessage.GeneratedExtension<M, List<T>>, index: Int
): T? = if (index < getExtensionCount(extension)) getExtension(extension, index) else null
