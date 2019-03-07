/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.roots

import com.intellij.conversion.*
import com.intellij.conversion.impl.ConversionContextImpl
import com.intellij.conversion.impl.ModuleSettingsImpl
import com.intellij.openapi.roots.ExternalProjectSystemRegistry
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.ContentEntryImpl
import com.intellij.openapi.roots.impl.OrderEntryFactory.ORDER_ENTRY_TYPE_ATTR
import com.intellij.openapi.roots.impl.SourceFolderImpl
import com.intellij.openapi.roots.impl.libraries.ApplicationLibraryTable
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.impl.libraries.LibraryImpl
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryKind
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.jdom.Element
import org.jetbrains.jps.model.JpsElement
import org.jetbrains.jps.model.JpsElementFactory
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import org.jetbrains.jps.model.module.JpsTypedModuleSourceRoot
import org.jetbrains.jps.model.serialization.facet.JpsFacetSerializer
import org.jetbrains.jps.model.serialization.module.JpsModuleRootModelSerializer
import org.jetbrains.jps.model.serialization.module.JpsModuleRootModelSerializer.*
import org.jetbrains.kotlin.config.getFacetPlatformByConfigurationElement
import org.jetbrains.kotlin.idea.facet.KotlinFacetType
import org.jetbrains.kotlin.idea.framework.*
import org.jetbrains.kotlin.idea.refactoring.toVirtualFile
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.utils.PathUtil

class KotlinNonJvmSourceRootConverterProvider : ConverterProvider("kotlin-non-jvm-source-roots") {
    companion object {
        private val rootTypesToMigrate: List<JpsModuleSourceRootType<*>> = listOf(
                JavaSourceRootType.SOURCE,
                JavaSourceRootType.TEST_SOURCE,
                JavaResourceRootType.RESOURCE,
                JavaResourceRootType.TEST_RESOURCE
        )

        private val TargetPlatform.stdlibDetector: ((Array<VirtualFile>) -> Boolean)?
            get() = when {
                isJvm() -> { roots -> JavaRuntimeDetectionUtil.getRuntimeJar(roots.toList()) != null }
                isJs() -> { roots -> JsLibraryStdDetectionUtil.getJsStdLibJar(roots.toList()) != null }
                isCommon() -> { roots -> getLibraryJar(roots, PathUtil.KOTLIN_STDLIB_COMMON_JAR_PATTERN) != null }
                else -> null
            }
    }

    sealed class LibInfo {
        class ByXml(
            private val element: Element,
            private val conversionContext: ConversionContext,
            private val moduleSettings: ModuleSettings
        ) : LibInfo() {
            override val explicitKind: PersistentLibraryKind<*>?
                get() = LibraryKind.findById(element.getAttributeValue("type")) as? PersistentLibraryKind<*>

            override fun getRoots(): Array<VirtualFile> {
                val contextImpl = conversionContext as? ConversionContextImpl ?: return VirtualFile.EMPTY_ARRAY
                val moduleSettingsImpl = moduleSettings as? ModuleSettingsImpl ?: return VirtualFile.EMPTY_ARRAY
                return contextImpl
                    .getClassRoots(element, moduleSettingsImpl)
                    .mapNotNull { it.toVirtualFile()?.let { JarFileSystem.getInstance().getJarRootForLocalFile(it) } }
                    .toTypedArray()
            }
        }

        class ByLibrary(private val library: Library) : LibInfo() {
            override val explicitKind: PersistentLibraryKind<*>?
                get() = (library as? LibraryEx)?.kind

            override fun getRoots() = library.getFiles(OrderRootType.CLASSES)
        }

        abstract val explicitKind: PersistentLibraryKind<*>?
        abstract fun getRoots(): Array<VirtualFile>

        val platform by lazy {
            val explicitKind = explicitKind
            val kind = if (explicitKind is KotlinLibraryKind) explicitKind else detectLibraryKind(getRoots())
            kind?.platform ?: DefaultBuiltInPlatforms.jvmPlatform // TODO: provide proper default
        }

        val isStdlib: Boolean
            get() = platform.stdlibDetector?.invoke(getRoots()) ?: false
    }

    class ConverterImpl(private val context: ConversionContext) : ProjectConverter() {
        private val projectLibrariesByName by lazy {
            context.projectLibrariesSettings.projectLibraries.groupBy { it.getAttributeValue(LibraryImpl.LIBRARY_NAME_ATTR) }
        }

        private fun findGlobalLibrary(name: String) = ApplicationLibraryTable.getApplicationTable().getLibraryByName(name)

        private fun findProjectLibrary(name: String) = projectLibrariesByName[name]?.firstOrNull()

        private fun createLibInfo(orderEntryElement: Element, moduleSettings: ModuleSettings): LibInfo? {
            val entryType = orderEntryElement.getAttributeValue(ORDER_ENTRY_TYPE_ATTR)
            return when (entryType) {
                JpsModuleRootModelSerializer.MODULE_LIBRARY_TYPE -> {
                    orderEntryElement.getChild(LIBRARY_TAG)?.let { LibInfo.ByXml(it, context, moduleSettings) }
                }

                JpsModuleRootModelSerializer.LIBRARY_TYPE -> {
                    val libraryName = orderEntryElement.getAttributeValue(NAME_ATTRIBUTE) ?: return null
                    val level = orderEntryElement.getAttributeValue(LEVEL_ATTRIBUTE)
                    when (level) {
                        LibraryTablesRegistrar.PROJECT_LEVEL ->
                            findProjectLibrary(libraryName)?.let { LibInfo.ByXml(it, context, moduleSettings) }
                        LibraryTablesRegistrar.APPLICATION_LEVEL ->
                            findGlobalLibrary(libraryName)?.let { LibInfo.ByLibrary(it) }
                        else ->
                            null
                    }
                }

                else -> null
            }
        }

        override fun createModuleFileConverter(): ConversionProcessor<ModuleSettings> {
            return object : ConversionProcessor<ModuleSettings>() {
                private fun ModuleSettings.detectPlatformByFacet() =
                    getFacetElement(KotlinFacetType.ID)
                        ?.getChild(JpsFacetSerializer.CONFIGURATION_TAG)
                        ?.getFacetPlatformByConfigurationElement()
                        ?.kind?.compilerPlatform

                private fun ModuleSettings.detectPlatformByDependencies(): TargetPlatform? {
                    var hasCommonStdlib = false

                    orderEntries
                        .asSequence()
                        .mapNotNull { createLibInfo(it, this) }
                        .forEach {
                            val platform = it.platform
                            when {
                                platform.isCommon() -> {
                                    if (!hasCommonStdlib && it.isStdlib) {
                                        hasCommonStdlib = true
                                    }
                                }

                                else -> {
                                    if (it.isStdlib) return platform
                                }
                            }
                        }

                    return if (hasCommonStdlib) DefaultBuiltInPlatforms.commonPlatform else null
                }

                private fun ModuleSettings.detectPlatform(): TargetPlatform {
                    return detectPlatformByFacet()
                            ?: detectPlatformByDependencies()
                            ?: DefaultBuiltInPlatforms.jvmPlatform
                }

                private fun ModuleSettings.getSourceFolderElements(): List<Element> {
                    val rootManagerElement = getComponentElement(ModuleSettings.MODULE_ROOT_MANAGER_COMPONENT) ?: return emptyList()
                    return rootManagerElement
                        .getChildren(ContentEntryImpl.ELEMENT_NAME)
                        .flatMap { it.getChildren(SourceFolderImpl.ELEMENT_NAME) }
                }

                private fun ModuleSettings.isExternalModule(): Boolean {
                    return when {
                        rootElement.getAttributeValue(ExternalProjectSystemRegistry.EXTERNAL_SYSTEM_ID_KEY) != null -> true
                        rootElement.getAttributeValue(ExternalProjectSystemRegistry.IS_MAVEN_MODULE_KEY)?.toBoolean() ?: false -> true
                        else -> false
                    }
                }

                override fun isConversionNeeded(settings: ModuleSettings): Boolean {
                    if (settings.isExternalModule()) return false

                    val targetPlatform = settings.detectPlatform()
                    if (targetPlatform.isJvm()) return false

                    return settings.getSourceFolderElements().any {
                        JpsModuleRootModelSerializer.loadSourceRoot(it).rootType in rootTypesToMigrate
                    }
                }

                override fun process(settings: ModuleSettings) {
                    for (sourceFolder in settings.getSourceFolderElements()) {
                        val contentRoot = sourceFolder.parent as? Element ?: continue
                        val oldSourceRoot = JpsModuleRootModelSerializer.loadSourceRoot(sourceFolder)
                        val url = sourceFolder.getAttributeValue(JpsModuleRootModelSerializer.URL_ATTRIBUTE)

                        val (newRootType, data) = oldSourceRoot.getMigratedSourceRootTypeWithProperties() ?: continue
                        @Suppress("UNCHECKED_CAST")
                        val newSourceRoot = JpsElementFactory.getInstance().createModuleSourceRoot(url, newRootType, data)
                                as? JpsTypedModuleSourceRoot<JpsElement> ?: continue

                        contentRoot.removeContent(sourceFolder)
                        JpsModuleRootModelSerializer.saveSourceRoot(contentRoot, url, newSourceRoot)
                    }
                }
            }
        }
    }

    override fun getConversionDescription() = "Update source roots for non-JVM modules in Kotlin project"

    override fun createConverter(context: ConversionContext) = ConverterImpl(context)
}