/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.project.structure

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.ide.plugins.PluginXmlPathResolver
import com.intellij.ide.plugins.RawPluginDescriptor
import com.intellij.ide.plugins.ReadModuleContext
import com.intellij.mock.MockProject
import com.intellij.openapi.extensions.DefaultPluginDescriptor
import com.intellij.platform.util.plugins.DataLoader
import com.intellij.util.NoOpXmlInterner
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.lang.ZipFilePool
import com.intellij.util.messages.ListenerDescriptor
import com.intellij.util.messages.impl.MessageBusEx
import org.jetbrains.kotlin.analysis.api.KtAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.providers.analysisMessageBus
import org.jetbrains.kotlin.utils.SmartList
import java.io.InputStream

/**
 * The Analysis API has an XML-based declaration way to define services, extensions and so on.
 *
 * This class provides a way to register such definitions to avoid manual registration in [AnalysisApiStandaloneServiceRegistrar].
 */
@Suppress("UnstableApiUsage")
@KtAnalysisNonPublicApi
object PluginStructureProvider {
    /**
     * This fake plugin is required to provide it as a required parameter.
     * Effectively, it is only used to group project listeners.
     */
    private val fakePluginDescriptor = DefaultPluginDescriptor("analysis-api-standalone-base-loader")

    private object ReadContext : ReadModuleContext {
        override val interner get() = NoOpXmlInterner
        override val isMissingIncludeIgnored: Boolean get() = false
    }

    private class ResourceDataLoader(val classLoader: ClassLoader) : DataLoader {
        override val pool: ZipFilePool? get() = null
        override fun load(path: String): InputStream? = classLoader.getResource(path)?.openStream()
        override fun toString(): String = "resources data loader"
    }

    private val pluginDescriptorsCache = ContainerUtil.createConcurrentSoftKeySoftValueMap<PluginDesignation, RawPluginDescriptor>()

    private data class PluginDesignation(val relativePath: String, val classLoader: ClassLoader) {
        constructor(relativePath: String, project: MockProject) : this(
            relativePath,
            project.loadClass<Any>(PluginDesignation::class.java.name, fakePluginDescriptor).classLoader,
        )
    }

    private fun getOrCalculatePluginDescriptor(
        designation: PluginDesignation,
    ): RawPluginDescriptor = pluginDescriptorsCache.computeIfAbsent(designation) {
        val descriptor = RawPluginDescriptor()
        PluginXmlPathResolver.DEFAULT_PATH_RESOLVER.resolvePath(
            readContext = ReadContext,
            dataLoader = ResourceDataLoader(designation.classLoader),
            relativePath = designation.relativePath,
            readInto = descriptor,
        )

        descriptor
    }

    fun registerProjectExtensionPoints(project: MockProject, pluginRelativePath: String) {
        val pluginDescriptor = getOrCalculatePluginDescriptor(PluginDesignation(pluginRelativePath, project))
        for (extensionPointDescriptor in pluginDescriptor.projectContainerDescriptor.extensionPoints.orEmpty()) {
            CoreApplicationEnvironment.registerExtensionPoint(
                project.extensionArea,
                extensionPointDescriptor.name,
                project.loadClass<Any>(extensionPointDescriptor.className, fakePluginDescriptor),
            )
        }
    }

    fun registerProjectServices(project: MockProject, pluginRelativePath: String) {
        val pluginDescriptor = getOrCalculatePluginDescriptor(PluginDesignation(pluginRelativePath, project))
        for (serviceDescriptor in pluginDescriptor.projectContainerDescriptor.services) {
            val serviceImplementationClass = project.loadClass<Any>(serviceDescriptor.serviceImplementation, fakePluginDescriptor)
            val serviceInterface = serviceDescriptor.serviceInterface
            if (serviceInterface != null) {
                val serviceInterfaceClass = project.loadClass<Any>(serviceInterface, fakePluginDescriptor)

                @Suppress("UNCHECKED_CAST")
                project.registerServiceWithInterface(serviceInterfaceClass, serviceImplementationClass)
            } else {
                project.registerService(serviceImplementationClass)
            }
        }
    }

    fun registerProjectListeners(project: MockProject, pluginRelativePath: String) {
        val pluginDescriptor = getOrCalculatePluginDescriptor(PluginDesignation(pluginRelativePath, project))
        val listenerDescriptors = pluginDescriptor.projectContainerDescriptor.listeners.orEmpty().ifEmpty {
            return
        }

        val listenersMap = mutableMapOf<String, MutableList<ListenerDescriptor>>()
        for (listenerDescriptor in listenerDescriptors) {
            listenerDescriptor.pluginDescriptor = fakePluginDescriptor
            listenersMap.computeIfAbsent(listenerDescriptor.topicClassName) { SmartList() }.add(listenerDescriptor)
        }

        (project.analysisMessageBus as MessageBusEx).setLazyListeners(listenersMap)
    }

    // workaround for ambiguity resolution
    private fun <T> MockProject.registerServiceWithInterface(interfaceClass: Class<T>, implementationClass: Class<T>) {
        registerService(interfaceClass, implementationClass)
    }
}
