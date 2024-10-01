/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.load.kotlin.MetadataFinderFactory
import org.jetbrains.kotlin.serialization.deserialization.KotlinMetadataFinder

class CliMetadataFinderFactory(private val fileFinderFactory: CliVirtualFileFinderFactory) : MetadataFinderFactory {
    override fun create(scope: GlobalSearchScope): KotlinMetadataFinder =
        fileFinderFactory.create(scope)

    override fun create(project: Project, module: ModuleDescriptor): KotlinMetadataFinder =
        fileFinderFactory.create(project, module)
}
