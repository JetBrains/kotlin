package com.jetbrains.cidr.apple.gradle

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.idea.util.CopyableDataNodeUserDataProperty

var DataNode<out ModuleData>.appleSourceSet: AppleTargetModel?
        by CopyableDataNodeUserDataProperty(Key.create("APPLE_SOURCE_SET"))
