/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.config

import org.jetbrains.jps.model.serialization.JpsModelSerializerExtension
import org.jetbrains.jps.model.serialization.module.JpsModuleSourceRootPropertiesSerializer

open class KotlinCommonJpsModelSerializerExtension : JpsModelSerializerExtension() {
    override fun getModuleSourceRootPropertiesSerializers(): List<JpsModuleSourceRootPropertiesSerializer<*>> = listOf(
        KotlinSourceRootPropertiesSerializer.Source,
        KotlinSourceRootPropertiesSerializer.TestSource,
        KotlinResourceRootPropertiesSerializer.Resource,
        KotlinResourceRootPropertiesSerializer.TestResource
    )
}