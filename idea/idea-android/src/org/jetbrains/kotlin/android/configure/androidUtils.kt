/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.configure

import com.android.tools.idea.gradle.project.model.AndroidModuleModel
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.module.Module
import org.jetbrains.android.facet.AndroidFacet

fun Module.getAndroidModel(modelsProvider: IdeModifiableModelsProvider? = null): AndroidModuleModel? {
    if (modelsProvider == null) return AndroidModuleModel.get(this)
    val androidFacet = modelsProvider.getModifiableFacetModel(this).getFacetByType(AndroidFacet.getFacetType().id) ?: return null
    return AndroidModuleModel.get(androidFacet)
}