/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.roots.ui

import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ui.configuration.*
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.config.KotlinResourceRootType
import org.jetbrains.kotlin.config.KotlinSourceRootType

class KotlinContentEntriesEditor(
    moduleName: String,
    state: ModuleConfigurationState
) : CommonContentEntriesEditor(
        moduleName,
        state,
        KotlinSourceRootType.Source,
        KotlinSourceRootType.TestSource,
        KotlinResourceRootType.Resource,
        KotlinResourceRootType.TestResource
) {
    private val javaEditor by lazy {
        object : JavaContentEntriesEditor(moduleName, state) {
            public override fun addContentEntries(files: Array<out VirtualFile>?) = super.addContentEntries(files)
        }
    }

    override fun createContentEntryEditor(contentEntryUrl: String): ContentEntryEditor {
        return object : JavaContentEntryEditor(contentEntryUrl, editHandlers) {
            override fun getModel() = this@KotlinContentEntriesEditor.model
        }
    }

    override fun addContentEntries(files: Array<out VirtualFile>?): MutableList<ContentEntry> = javaEditor.addContentEntries(files)
}