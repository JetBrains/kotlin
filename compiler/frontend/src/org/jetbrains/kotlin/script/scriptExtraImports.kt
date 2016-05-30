/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.script

import com.intellij.openapi.util.JDOMUtil
import com.intellij.util.xmlb.XmlSerializer
import com.intellij.util.xmlb.annotations.AbstractCollection
import com.intellij.util.xmlb.annotations.Tag
import java.io.File
import java.io.InputStream
import java.util.*

/**
 *  A particular script dependency, additional to script kind dependencies defined by KotlinScriptDefinition
 */
interface KotlinScriptExtraImport {
    val classpath: List<String>
    val names: List<String>
}

// -----

@Tag("import")
class KotlinScriptExtraImportConfig {
    @Tag("classpath")
    @AbstractCollection(surroundWithTag = false, elementTag = "path", elementValueAttribute = "")
    var classpath: MutableList<String> = ArrayList()

    @Tag("names")
    @AbstractCollection(surroundWithTag = false, elementTag = "name", elementValueAttribute = "")
    var names: MutableList<String> = ArrayList()
}

fun loadScriptExtraImportConfigs(configFile: File): List<KotlinScriptExtraImportConfig> =
        JDOMUtil.loadDocument(configFile).rootElement.children.mapNotNull {
            XmlSerializer.deserialize(it, KotlinScriptExtraImportConfig::class.java)
        }

fun loadScriptExtraImportConfigs(configStream: InputStream): List<KotlinScriptExtraImportConfig> =
        JDOMUtil.loadDocument(configStream).rootElement.children.mapNotNull {
            XmlSerializer.deserialize(it, KotlinScriptExtraImportConfig::class.java)
        }

class KotlinScriptExtraImportFromConfig(val config : KotlinScriptExtraImportConfig, val envVars: Map<String, List<String>>) : KotlinScriptExtraImport {
    override val classpath: List<String> by lazy { config.classpath.evalWithVars(envVars).distinct() }
    override val names: List<String>
        get() = config.names
}

