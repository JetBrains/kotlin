/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.test

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModifiableRootModel

class KotlinMultiModuleProjectDescriptor(
    val id: String,
    private val mainModuleDescriptor: KotlinLightProjectDescriptor,
    private val additionalModuleDescriptor: KotlinLightProjectDescriptor
) : KotlinLightProjectDescriptor() {
    lateinit var additionalModule: Module

    override fun setUpProject(project: Project, handler: SetupHandler) {
        super.setUpProject(project, handler)

        additionalModule = additionalModuleDescriptor.createMainModule(project)
    }

    override fun configureModule(module: Module, model: ModifiableRootModel) {
        mainModuleDescriptor.configureModule(module, model)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KotlinMultiModuleProjectDescriptor

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}