/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.internal

import com.intellij.codeInspection.LocalInspectionEP
import com.intellij.ide.util.TipAndTrickBean
import com.intellij.openapi.extensions.Extensions

/**
 * @author Vladislav.Soroka
 */
class KotlinNativeIdeInitializer {

    init {
        unregisterGroovyInspections()
        suppressKotlinJvmTipsAndTricks()
    }

    // There are groovy local inspections which should not be loaded w/o groovy plugin enabled.
    // Those plugin definitions should become optional and dependant on groovy plugin.
    // This is a temp workaround before it happens.
    private fun unregisterGroovyInspections() {
        val extensionPoint = Extensions.getRootArea().getExtensionPoint(LocalInspectionEP.LOCAL_INSPECTION)
        extensionPoint.extensions.filter { it.groupDisplayName == "Kotlin" && it.language == "Groovy" }.forEach {
            extensionPoint.unregisterExtension(it)
        }
    }

    private fun suppressKotlinJvmTipsAndTricks() {
        val extensionPoint = Extensions.getRootArea().getExtensionPoint(TipAndTrickBean.EP_NAME)
        for (name in arrayOf("Kotlin.html", "Kotlin_project.html", "Kotlin_mix.html", "Kotlin_Java_convert.html")) {
            TipAndTrickBean.findByFileName(name)?.let {
                extensionPoint.unregisterExtension(it)
            }
        }
    }
}
