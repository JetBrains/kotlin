/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import org.jetbrains.jps.model.ex.JpsElementTypeBase
import org.jetbrains.jps.model.java.JavaResourceRootProperties
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.model.module.JpsModuleSourceRootType

sealed class KotlinResourceRootType : JpsElementTypeBase<JavaResourceRootProperties>(),
        JpsModuleSourceRootType<JavaResourceRootProperties> {
    object Resource : KotlinResourceRootType()
    object TestResource : KotlinResourceRootType()

    override fun createDefaultProperties() =
        JpsJavaExtensionService.getInstance().createResourceRootProperties("", false)
}