/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData

@Suppress("UNCHECKED_CAST")
fun DataNode<*>.findChildModuleById(id: String) =
    children.firstOrNull { (it.data as? ModuleData)?.id == id } as? DataNode<out ModuleData>

@Suppress("UNCHECKED_CAST")
fun DataNode<*>.findChildModuleByInternalName(name: String) =
    children.firstOrNull { (it.data as? ModuleData)?.internalName == name } as? DataNode<out ModuleData>