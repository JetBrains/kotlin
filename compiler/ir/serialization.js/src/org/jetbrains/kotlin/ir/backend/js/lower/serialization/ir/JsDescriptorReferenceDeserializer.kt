/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.common.serialization.DescriptorReferenceDeserializer
import org.jetbrains.kotlin.backend.common.serialization.DescriptorUniqIdAware
import org.jetbrains.kotlin.backend.common.serialization.DeserializedDescriptorUniqIdAware
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.KotlinMangler

class JsDescriptorReferenceDeserializer(
    currentModule: ModuleDescriptor,
    mangler: KotlinMangler,
    builtIns: IrBuiltIns
) : DescriptorReferenceDeserializer(currentModule, mangler, builtIns, mutableMapOf()),
    DescriptorUniqIdAware by DeserializedDescriptorUniqIdAware
