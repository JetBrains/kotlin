/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core

import com.intellij.openapi.roots.FileIndex
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import org.jetbrains.kotlin.config.KotlinResourceRootType
import org.jetbrains.kotlin.config.KotlinSourceRootType

private val testRootTypes: Set<JpsModuleSourceRootType<*>> = setOf(
        JavaSourceRootType.TEST_SOURCE,
        JavaResourceRootType.TEST_RESOURCE,
        KotlinSourceRootType.TestSource,
        KotlinResourceRootType.TestResource
)

fun FileIndex.isInTestSourceContentKotlinAware(fileOrDir: VirtualFile) = isUnderSourceRootOfType(fileOrDir, testRootTypes)