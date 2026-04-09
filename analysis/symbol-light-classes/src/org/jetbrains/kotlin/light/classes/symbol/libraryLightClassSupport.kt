/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.registry.Registry
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind

private const val SYMBOL_LIGHT_CLASSES_FOR_LIBRARIES_REGISTRY_KEY: String = "kotlin.symbol.light.classes.for.libraries"

private val libraryLightClassesTestOverrideKey = Key.create<Boolean>("kotlin.symbol.light.classes.for.libraries.test.override")

internal fun Project.areSymbolLightClassesEnabledForLibraries(): Boolean {
    return getUserData(libraryLightClassesTestOverrideKey) ?: Registry.`is`(
        /* key = */ SYMBOL_LIGHT_CLASSES_FOR_LIBRARIES_REGISTRY_KEY,
        /* defaultValue = */ false,
    )
}

internal fun Project.setSymbolLightClassesEnabledForLibrariesForTests(enabled: Boolean) {
    putUserData(libraryLightClassesTestOverrideKey, enabled)
}

internal fun KaModule.lightClassOriginKind(): LightClassOriginKind = when (this) {
    is KaLibraryModule -> LightClassOriginKind.BINARY
    else -> LightClassOriginKind.SOURCE
}
