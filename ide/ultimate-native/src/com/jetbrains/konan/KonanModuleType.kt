/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.konan

import com.intellij.icons.AllIcons
import com.intellij.ide.util.projectWizard.EmptyModuleBuilder
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.ModuleTypeManager
import com.intellij.openapi.module.impl.ModuleTypeManagerImpl
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.module.JpsModuleSourceRootType

class KonanModuleType : ModuleType<EmptyModuleBuilder>(KonanBundle.message("id.module")) {
    override fun createModuleBuilder(): EmptyModuleBuilder {
        return object : EmptyModuleBuilder() {
            override fun getModuleType(): ModuleType<*> {
                return ModuleTypeManager.getInstance().findByID(KonanBundle.message("id.module"))
            }
        }
    }

    override fun getName() = KonanBundle.message("label.moduleName.text")

    override fun getDescription() = KonanBundle.message("label.moduleDescription.text")

    override fun getNodeIcon(isOpened: Boolean) = AllIcons.Modules.SourceFolder

    override fun isSupportedRootType(type: JpsModuleSourceRootType<*>): Boolean {
        return type !== JavaSourceRootType.TEST_SOURCE
    }
}

class KonanModuleTypeManager : ModuleTypeManagerImpl() {
    override fun getDefaultModuleType(): ModuleType<*> {
        return KonanModuleType()
    }
}