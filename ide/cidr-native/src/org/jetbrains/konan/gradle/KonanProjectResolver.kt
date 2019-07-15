/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.jetbrains.konan.KONAN_MODEL_KEY
import com.jetbrains.konan.KonanModel
import com.jetbrains.konan.KonanModelImpl
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension

class KonanProjectResolver : AbstractProjectResolverExtension() {

    override fun getToolingExtensionsClasses(): Set<Class<out Any>> = setOf(KonanModelBuilder::class.java, Unit::class.java)

    override fun getExtraProjectModelClasses(): Set<Class<out Any>> = setOf(KonanModel::class.java)

    override fun populateModuleExtraModels(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        resolverCtx.getExtraProject(gradleModule, KonanModel::class.java)?.let {
            // store a local process copy of the object to get rid of proxy types for further serialization
            ideModule.createChild(KONAN_MODEL_KEY, KonanModelImpl(it))
        }

        nextResolver.populateModuleExtraModels(gradleModule, ideModule)
    }
}