/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import org.jetbrains.jps.model.ex.JpsElementTypeBase
import org.jetbrains.jps.model.java.JavaSourceRootProperties
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.model.module.JpsModuleSourceRootType

sealed class KotlinSourceRootType(val isTest: Boolean) : JpsElementTypeBase<JavaSourceRootProperties>(), JpsModuleSourceRootType<JavaSourceRootProperties>, KotlinRootType {
    object Source : KotlinSourceRootType(false)
    object TestSource : KotlinSourceRootType(true)

    override fun createDefaultProperties() = JpsJavaExtensionService.getInstance().createSourceRootProperties("")

    companion object {
        val ALL_SOURCES = setOf(Source, TestSource)
    }

    override fun isTestRoot() = isTest

    override fun equals(other: Any?) = if (super.equals(other)) true else isSameRootType(this, other)
}

