/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.config

import com.intellij.util.PathUtil
import com.intellij.util.xmlb.SerializationFilter
import com.intellij.util.xmlb.SkipDefaultsSerializationFilter
import com.intellij.util.xmlb.XmlSerializer
import org.jdom.DataConversionException
import org.jdom.Element
import org.jdom.Text
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.platform.*
import org.jetbrains.kotlin.platform.impl.FakeK2NativeCompilerArguments
import org.jetbrains.kotlin.platform.impl.JvmIdePlatformKind
import org.jetbrains.kotlin.platform.js.JsPlatform
import org.jetbrains.kotlin.platform.jvm.JdkPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatform
import org.jetbrains.kotlin.platform.konan.KonanPlatform
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

fun Element.getOption(name: String) = getChildren("option").firstOrNull { it.getAttribute("name").value == name }

private fun Element.getOptionValue(name: String) = getOption(name)?.getAttribute("value")?.value

private fun Element.getOptionBody(name: String) = getOption(name)?.children?.firstOrNull()

fun TargetPlatform.createArguments(init: (CommonCompilerArguments).() -> Unit = {}): CommonCompilerArguments {
    return when (val singlePlatform = singleOrNull()) {
        null -> K2MetadataCompilerArguments().apply { init() }
        is JvmPlatform -> K2JVMCompilerArguments().apply {
            init()
            // TODO(dsavvinov): review this
            jvmTarget = (singlePlatform as? JdkPlatform)?.targetVersion?.description ?: JvmTarget.DEFAULT.description
        }
        is JsPlatform -> K2JSCompilerArguments().apply { init() }
        is KonanPlatform -> FakeK2NativeCompilerArguments().apply { init() }
        else -> error("Unknown platform $singlePlatform")
    }
}

private fun readV1Config(element: Element): KotlinFacetSettings {
    return KotlinFacetSettings().apply {
        val useProjectSettings = element.getOptionValue("useProjectSettings")?.toBoolean()

        val versionInfoElement = element.getOptionBody("versionInfo")
        val targetPlatformName = versionInfoElement?.getOptionValue("targetPlatformName")
        val languageLevel = versionInfoElement?.getOptionValue("languageLevel")
        val apiLevel = versionInfoElement?.getOptionValue("apiLevel")
        val targetPlatform = CommonPlatforms.allSimplePlatforms.union(setOf(CommonPlatforms.defaultCommonPlatform))
            .firstOrNull { it.oldFashionedDescription == targetPlatformName }
            ?: JvmIdePlatformKind.defaultPlatform // FIXME(dsavvinov): choose proper default

        val compilerInfoElement = element.getOptionBody("compilerInfo")

        val compilerSettings = CompilerSettings().apply {
            compilerInfoElement?.getOptionBody("compilerSettings")?.let { compilerSettingsElement ->
                XmlSerializer.deserializeInto(this, compilerSettingsElement)
            }
        }

        val commonArgumentsElement = compilerInfoElement?.getOptionBody("_commonCompilerArguments")
        val jvmArgumentsElement = compilerInfoElement?.getOptionBody("k2jvmCompilerArguments")
        val jsArgumentsElement = compilerInfoElement?.getOptionBody("k2jsCompilerArguments")

        val compilerArguments = targetPlatform.createArguments { freeArgs = arrayListOf() }

        commonArgumentsElement?.let { XmlSerializer.deserializeInto(compilerArguments, it) }
        when (compilerArguments) {
            is K2JVMCompilerArguments -> jvmArgumentsElement?.let { XmlSerializer.deserializeInto(compilerArguments, it) }
            is K2JSCompilerArguments -> jsArgumentsElement?.let { XmlSerializer.deserializeInto(compilerArguments, it) }
        }

        if (languageLevel != null) {
            compilerArguments.languageVersion = languageLevel
        }

        if (apiLevel != null) {
            compilerArguments.apiVersion = apiLevel
        }

        compilerArguments.detectVersionAutoAdvance()

        if (useProjectSettings != null) {
            this.useProjectSettings = useProjectSettings
        }
        else {
            // Migration problem workaround for pre-1.1-beta releases (mainly 1.0.6) -> 1.1-rc+
            // Problematic cases: 1.1-beta/1.1-beta2 -> 1.1-rc+ (useProjectSettings gets reset to false)
            // This heuristic detects old enough configurations:
            if (jvmArgumentsElement == null) {
                this.useProjectSettings = false
            }
        }

        this.compilerSettings = compilerSettings
        this.compilerArguments = compilerArguments
        this.targetPlatform = IdePlatformKind.platformByCompilerArguments(compilerArguments)
    }
}

fun Element.getFacetPlatformByConfigurationElement(): TargetPlatform {
    val platformNames = getAttributeValue("allPlatforms")?.split('/')?.toSet()
    if (platformNames != null) {
        return TargetPlatform(CommonPlatforms.allSimplePlatforms
                                  .flatMap { it.componentPlatforms }
                                  .filter { platformNames.contains(it.serializeToString()) }
                                  .toSet())
    }
    // failed to read list of all platforms. Fallback to legacy algorythm
    val platformName = getAttributeValue("platform")
    // this code could be simplified using union after fixing the equals method in SimplePlatform
    val allPlatforms = ArrayList(CommonPlatforms.allSimplePlatforms).also { it.add(CommonPlatforms.defaultCommonPlatform) }
    return allPlatforms.firstOrNull { it.oldFashionedDescription == platformName }.orDefault()
}

private fun readV2AndLaterConfig(element: Element): KotlinFacetSettings {
    return KotlinFacetSettings().apply {
        element.getAttributeValue("useProjectSettings")?.let { useProjectSettings = it.toBoolean() }
        val targetPlatform = element.getFacetPlatformByConfigurationElement()
        this.targetPlatform = targetPlatform
        element.getChild("implements")?.let {
            val items = it.getChildren("implement")
            implementedModuleNames = if (items.isNotEmpty()) {
                items.mapNotNull { (it.content.firstOrNull() as? Text)?.textTrim }
            } else {
                listOfNotNull((it.content.firstOrNull() as? Text)?.textTrim)
            }
        }
        element.getChild("sourceSets")?.let {
            val items = it.getChildren("sourceSet")
            sourceSetNames = items.mapNotNull { (it.content.firstOrNull() as? Text)?.textTrim }
        }
        kind = element.getChild("newMppModelJpsModuleKind")?.let {
            val kindName = (it.content.firstOrNull() as? Text)?.textTrim
            if (kindName != null) {
                try {
                    KotlinModuleKind.valueOf(kindName)
                } catch (e: Exception) {
                    null
                }
            } else null
        } ?: KotlinModuleKind.DEFAULT
        isTestModule = element.getAttributeValue("isTestModule")?.toBoolean() ?: false
        externalProjectId = element.getAttributeValue("externalProjectId") ?: ""
        element.getChild("compilerSettings")?.let {
            compilerSettings = CompilerSettings()
            XmlSerializer.deserializeInto(compilerSettings!!, it)
        }
        element.getChild("compilerArguments")?.let {
            compilerArguments = targetPlatform.createArguments {
                freeArgs = mutableListOf()
                internalArguments = mutableListOf()
            }
            XmlSerializer.deserializeInto(compilerArguments!!, it)
            compilerArguments!!.detectVersionAutoAdvance()
        }
        productionOutputPath = element.getChild("productionOutputPath")?.let {
            PathUtil.toSystemDependentName((it.content.firstOrNull() as? Text)?.textTrim)
        } ?: (compilerArguments as? K2JSCompilerArguments)?.outputFile
        testOutputPath = element.getChild("testOutputPath")?.let {
            PathUtil.toSystemDependentName((it.content.firstOrNull() as? Text)?.textTrim)
        } ?: (compilerArguments as? K2JSCompilerArguments)?.outputFile
    }
}

private fun readV2Config(element: Element): KotlinFacetSettings {
    return readV2AndLaterConfig(element).apply {
        element.getChild("compilerArguments")?.children?.let { args ->
            when {
                args.any { arg -> arg.attributes[0].value == "coroutinesEnable" && arg.attributes[1].booleanValue } ->
                    compilerArguments!!.coroutinesState = CommonCompilerArguments.ENABLE
                args.any { arg -> arg.attributes[0].value == "coroutinesWarn" && arg.attributes[1].booleanValue } ->
                    compilerArguments!!.coroutinesState = CommonCompilerArguments.WARN
                args.any { arg -> arg.attributes[0].value == "coroutinesError" && arg.attributes[1].booleanValue } ->
                    compilerArguments!!.coroutinesState = CommonCompilerArguments.ERROR
                else -> compilerArguments!!.coroutinesState = CommonCompilerArguments.DEFAULT
            }
        }
    }
}

private fun readLatestConfig(element: Element): KotlinFacetSettings {
    return readV2AndLaterConfig(element)
}

fun deserializeFacetSettings(element: Element): KotlinFacetSettings {
    val version =
            try {
                element.getAttribute("version")?.intValue
            }
            catch(e: DataConversionException) {
                null
            } ?: KotlinFacetSettings.DEFAULT_VERSION
    return when (version) {
        1 -> readV1Config(element)
        2 -> readV2Config(element)
        KotlinFacetSettings.CURRENT_VERSION -> readLatestConfig(element)
        else -> return KotlinFacetSettings() // Reset facet configuration if versions don't match
    }.apply { this.version = version }
}

fun CommonCompilerArguments.convertPathsToSystemIndependent() {
    pluginClasspaths?.forEachIndexed { index, s -> pluginClasspaths!![index] = PathUtil.toSystemIndependentName(s) }

    when (this) {
        is K2JVMCompilerArguments -> {
            destination = PathUtil.toSystemIndependentName(destination)
            classpath = PathUtil.toSystemIndependentName(classpath)
            jdkHome = PathUtil.toSystemIndependentName(jdkHome)
            kotlinHome = PathUtil.toSystemIndependentName(kotlinHome)
            friendPaths?.forEachIndexed { index, s -> friendPaths!![index] = PathUtil.toSystemIndependentName(s) }
            declarationsOutputPath = PathUtil.toSystemIndependentName(declarationsOutputPath)
        }

        is K2JSCompilerArguments -> {
            outputFile = PathUtil.toSystemIndependentName(outputFile)
            libraries = PathUtil.toSystemIndependentName(libraries)
        }

        is K2MetadataCompilerArguments -> {
            destination = PathUtil.toSystemIndependentName(destination)
            classpath = PathUtil.toSystemIndependentName(classpath)
        }
    }
}

fun CompilerSettings.convertPathsToSystemIndependent() {
    scriptTemplatesClasspath = PathUtil.toSystemIndependentName(scriptTemplatesClasspath)
    outputDirectoryForJsLibraryFiles = PathUtil.toSystemIndependentName(outputDirectoryForJsLibraryFiles)
}

private fun KClass<*>.superClass() = superclasses.firstOrNull { !it.java.isInterface }

private fun Class<*>.computeNormalPropertyOrdering(): Map<String, Int> {
    val result = LinkedHashMap<String, Int>()
    var count = 0
    generateSequence(this) { it.superclass }.forEach { clazz ->
        for (field in clazz.declaredFields) {
            if (field.modifiers and Modifier.STATIC != 0) continue

            var propertyName = field.name
            if (propertyName.endsWith(JvmAbi.DELEGATED_PROPERTY_NAME_SUFFIX)) {
                propertyName = propertyName.dropLast(JvmAbi.DELEGATED_PROPERTY_NAME_SUFFIX.length)
            }

            result[propertyName] = count++
        }
    }
    return result
}

private val allNormalOrderings = HashMap<Class<*>, Map<String, Int>>()

private val Class<*>.normalOrdering
    get() = synchronized(allNormalOrderings) { allNormalOrderings.getOrPut(this) { computeNormalPropertyOrdering() } }

// Replacing fields with delegated properties leads to unexpected reordering of entries in facet configuration XML
// It happens due to XmlSerializer using different orderings for field- and method-based accessors
// This code restores the original ordering
private fun Element.restoreNormalOrdering(bean: Any) {
    val normalOrdering = bean.javaClass.normalOrdering
    val elementsToReorder = this.getContent<Element> { it is Element && it.getAttribute("name")?.value in normalOrdering }
    elementsToReorder
            .sortedBy { normalOrdering[it.getAttribute("name")?.value!!] }
            .forEachIndexed { index, element -> elementsToReorder[index] = element.clone() }
}

private fun buildChildElement(element: Element, tag: String, bean: Any, filter: SerializationFilter): Element {
    return Element(tag).apply {
        XmlSerializer.serializeInto(bean, this, filter)
        restoreNormalOrdering(bean)
        element.addContent(this)
    }
}

private fun KotlinFacetSettings.writeLatestConfig(element: Element) {
    val filter = SkipDefaultsSerializationFilter()

    targetPlatform?.let {
        element.setAttribute("allPlatforms", it.componentPlatforms.map { it.serializeToString() }.joinToString(separator = "/"))
    }
    if (!useProjectSettings) {
        element.setAttribute("useProjectSettings", useProjectSettings.toString())
    }
    if (implementedModuleNames.isNotEmpty()) {
        element.addContent(
                Element("implements").apply {
                    val singleModule = implementedModuleNames.singleOrNull()
                    if (singleModule != null) {
                        addContent(singleModule)
                    } else {
                        implementedModuleNames.map { addContent(Element("implement").apply { addContent(it) }) }
                    }
                }
        )
    }
    if (sourceSetNames.isNotEmpty()) {
        element.addContent(
            Element("sourceSets").apply {
                sourceSetNames.map { addContent(Element("sourceSet").apply { addContent(it) }) }
            }
        )
    }
    if (kind != KotlinModuleKind.DEFAULT) {
        element.addContent(Element("newMppModelJpsModuleKind").apply { addContent(kind.name) })
        element.setAttribute("isTestModule", isTestModule.toString())
    }
    if (externalProjectId.isNotEmpty()) {
        element.setAttribute("externalProjectId", externalProjectId)
    }
    productionOutputPath?.let {
        if (it != (compilerArguments as? K2JSCompilerArguments)?.outputFile) {
            element.addContent(Element("productionOutputPath").apply { addContent(PathUtil.toSystemIndependentName(it)) })
        }
    }
    testOutputPath?.let {
        if (it != (compilerArguments as? K2JSCompilerArguments)?.outputFile) {
            element.addContent(Element("testOutputPath").apply { addContent(PathUtil.toSystemIndependentName(it)) })
        }
    }
    compilerSettings?.let { copyBean(it) }?.let {
        it.convertPathsToSystemIndependent()
        buildChildElement(element, "compilerSettings", it, filter)
    }
    compilerArguments?.let { copyBean(it) }?.let {
        it.convertPathsToSystemIndependent()
        val compilerArgumentsXml = buildChildElement(element, "compilerArguments", it, filter)
        compilerArgumentsXml.dropVersionsIfNecessary(it)
    }
}

fun CommonCompilerArguments.detectVersionAutoAdvance() {
    autoAdvanceLanguageVersion = languageVersion == null
    autoAdvanceApiVersion = apiVersion == null
}

fun Element.dropVersionsIfNecessary(settings: CommonCompilerArguments) {
    // Do not serialize language/api version if they correspond to the default language version
    if (settings.autoAdvanceLanguageVersion) {
        getOption("languageVersion")?.detach()
    }

    if (settings.autoAdvanceApiVersion) {
        getOption("apiVersion")?.detach()
    }
}

// Special treatment of v2 may be dropped after transition to IDEA 172
private fun KotlinFacetSettings.writeV2Config(element: Element) {
    writeLatestConfig(element)
    element.getChild("compilerArguments")?.let {
        it.getOption("coroutinesState")?.detach()
        val coroutineOption = when (compilerArguments?.coroutinesState) {
            CommonCompilerArguments.ENABLE -> "coroutinesEnable"
            CommonCompilerArguments.WARN -> "coroutinesWarn"
            CommonCompilerArguments.ERROR -> "coroutinesError"
            else -> null
        }
        if (coroutineOption != null) {
            Element("option").apply {
                setAttribute("name", coroutineOption)
                setAttribute("value", "true")
                it.addContent(this)
            }
        }
    }
}

fun KotlinFacetSettings.serializeFacetSettings(element: Element) {
    val versionToWrite = if (version == 2) version else KotlinFacetSettings.CURRENT_VERSION
    element.setAttribute("version", versionToWrite.toString())
    if (versionToWrite == 2) {
        writeV2Config(element)
    }
    else {
        writeLatestConfig(element)
    }
}
