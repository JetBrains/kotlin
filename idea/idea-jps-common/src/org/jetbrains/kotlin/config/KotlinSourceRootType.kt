/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import org.jetbrains.jps.model.ex.JpsElementTypeBase
import org.jetbrains.jps.model.java.JavaSourceRootProperties
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.model.module.JpsModuleSourceRootType

sealed class KotlinSourceRootType : JpsElementTypeBase<JavaSourceRootProperties>(), JpsModuleSourceRootType<JavaSourceRootProperties> {
    object Source : KotlinSourceRootType()
    object TestSource : KotlinSourceRootType()

    override fun createDefaultProperties() = JpsJavaExtensionService.getInstance().createSourceRootProperties("")
}

