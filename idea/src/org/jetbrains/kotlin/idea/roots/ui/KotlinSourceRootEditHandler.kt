/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.roots.ui

import com.intellij.openapi.roots.ui.configuration.*
import org.jetbrains.jps.model.JpsElement
import org.jetbrains.jps.model.java.JavaResourceRootProperties
import org.jetbrains.jps.model.java.JavaSourceRootProperties
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import org.jetbrains.kotlin.config.KotlinResourceRootType
import org.jetbrains.kotlin.config.KotlinSourceRootType

sealed class KotlinModuleSourceRootEditHandler<Data : JpsElement>(
    rootType: JpsModuleSourceRootType<Data>,
    private val delegate: ModuleSourceRootEditHandler<Data>
) : ModuleSourceRootEditHandler<Data>(rootType) {
    class Source : KotlinModuleSourceRootEditHandler<JavaSourceRootProperties>(
            KotlinSourceRootType.Source,
            JavaModuleSourceRootEditHandler()
    )

    class TestSource : KotlinModuleSourceRootEditHandler<JavaSourceRootProperties>(
            KotlinSourceRootType.TestSource,
            JavaTestSourceRootEditHandler()
    )

    class Resource : KotlinModuleSourceRootEditHandler<JavaResourceRootProperties>(
            KotlinResourceRootType.Resource,
            JavaResourceRootEditHandler()
    )

    class TestResource : KotlinModuleSourceRootEditHandler<JavaResourceRootProperties>(
            KotlinResourceRootType.TestResource,
            JavaTestResourceRootEditHandler()
    )

    override fun getUnmarkRootButtonText() = delegate.unmarkRootButtonText

    override fun getRootIcon() = delegate.rootIcon

    override fun getRootsGroupTitle() = delegate.rootsGroupTitle

    override fun getMarkRootShortcutSet() = delegate.markRootShortcutSet

    override fun getRootTypeName() = delegate.rootTypeName

    override fun getRootsGroupColor() = delegate.rootsGroupColor

    override fun getFolderUnderRootIcon() = delegate.folderUnderRootIcon
}