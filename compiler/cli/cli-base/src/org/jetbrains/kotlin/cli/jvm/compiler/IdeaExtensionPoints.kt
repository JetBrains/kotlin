/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.lang.jvm.facade.JvmElementProvider
import com.intellij.openapi.extensions.ExtensionsArea
import com.intellij.psi.JavaModuleSystem
import com.intellij.psi.impl.compiled.ClsCustomNavigationPolicy

internal object IdeaExtensionPoints {
    fun registerVersionSpecificAppExtensionPoints(area: ExtensionsArea) {
        @Suppress("DEPRECATION")
        CoreApplicationEnvironment.registerExtensionPoint(area, ClsCustomNavigationPolicy.EP_NAME, ClsCustomNavigationPolicy::class.java)
        CoreApplicationEnvironment.registerExtensionPoint(area, JavaModuleSystem.EP_NAME, JavaModuleSystem::class.java)
    }

    fun registerVersionSpecificProjectExtensionPoints(area: ExtensionsArea) {
        CoreApplicationEnvironment.registerExtensionPoint(area, JvmElementProvider.EP_NAME, JvmElementProvider::class.java)
    }
}
