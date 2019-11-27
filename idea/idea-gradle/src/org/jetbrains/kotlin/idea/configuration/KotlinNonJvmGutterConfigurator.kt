/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.util.Consumer
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension

class KotlinNonJvmGutterConfigurator : AbstractProjectResolverExtension() {
    override fun enhanceTaskProcessing(taskNames: MutableList<String>, jvmParametersSetup: String?, initScriptConsumer: Consumer<String>) {
        initScriptConsumer.consume(
            //language=Gradle
            """
                task nonJvmTestIdeSupport(type: Test) {
                }
            """.trimIndent()
        )
    }
}