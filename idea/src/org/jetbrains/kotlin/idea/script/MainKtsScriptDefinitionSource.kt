/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.script

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import kotlin.script.experimental.intellij.ScriptDefinitionsProvider

class MainKtsScriptDefinitionSource : ScriptDefinitionsProvider {
    override val id: String = ".main.kts script"

    override fun getDefinitionClasses(): Iterable<String> = emptyList()

    override fun getDefinitionsClassPath(): Iterable<File> {

        val paths = PathUtil.kotlinPathsForIdeaPlugin
        return if (paths.jar(KotlinPaths.Jar.MainKts).exists()) {
            paths.classPath(KotlinPaths.ClassPaths.MainKts)
        } else {
            Logger.getInstance(MainKtsScriptDefinitionSource::class.java).warn("[kts] Support for .main.kts scripts is not loaded: kotlin-main-kts.jar not found")
            emptyList()
        }
    }

    override fun useDiscovery(): Boolean = true
}