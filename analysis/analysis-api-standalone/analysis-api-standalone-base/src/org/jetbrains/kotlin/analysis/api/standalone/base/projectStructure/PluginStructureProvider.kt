/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.ide.plugins.ContainerDescriptor
import com.intellij.ide.plugins.DataLoader
import com.intellij.ide.plugins.PluginXmlPathResolver
import com.intellij.ide.plugins.RawPluginDescriptor
import com.intellij.ide.plugins.ReadModuleContext
import com.intellij.mock.MockApplication
import com.intellij.mock.MockComponentManager
import com.intellij.mock.MockProject
import com.intellij.openapi.extensions.DefaultPluginDescriptor
import com.intellij.openapi.extensions.PluginDescriptor
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.lang.ZipFilePool
import com.intellij.util.messages.ListenerDescriptor
import com.intellij.util.messages.impl.MessageBusEx
import com.intellij.util.xml.dom.NoOpXmlInterner
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.platform.analysisMessageBus
import org.jetbrains.kotlin.utils.SmartList
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * The Analysis API has an XML-based declaration way to define services, extensions and so on.
 *
 * This class provides a way to register such definitions to avoid manual registration in [AnalysisApiServiceRegistrar].
 */
@Suppress("UnstableApiUsage")
@KaImplementationDetail
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

        // KT-70264
        @Suppress("UNUSED")
        fun load(path: String, pluginDescriptorSourceOnly: Boolean): InputStream? = load(path)

        override fun toString(): String = "resources data loader"
    }

    private val pluginDescriptorsCache = ContainerUtil.createConcurrentSoftKeySoftValueMap<PluginDesignation, RawPluginDescriptor>()

    private data class PluginDesignation(val relativePath: String, val classLoader: ClassLoader) {
        constructor(relativePath: String, componentManager: MockComponentManager) : this(relativePath, componentManager.classLoader)
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

    fun registerApplicationServices(application: MockApplication, pluginRelativePath: String) {
        registerServices(application, pluginRelativePath, RawPluginDescriptor::appContainerDescriptor)
    }

    fun registerProjectServices(project: MockProject, pluginRelativePath: String) {
        registerServices(project, pluginRelativePath, RawPluginDescriptor::projectContainerDescriptor)
    }

    private inline fun registerServices(
        componentManager: MockComponentManager,
        pluginRelativePath: String,
        containerDescriptor: RawPluginDescriptor.() -> ContainerDescriptor,
    ) {
        val pluginDescriptor = getOrCalculatePluginDescriptor(PluginDesignation(pluginRelativePath, componentManager))
        for (serviceDescriptor in pluginDescriptor.containerDescriptor().services) {
            val serviceImplementationClass = componentManager.loadClass<Any>(serviceDescriptor.serviceImplementation, fakePluginDescriptor)
            val serviceInterface = serviceDescriptor.serviceInterface
            if (serviceInterface != null) {
                val serviceInterfaceClass = componentManager.loadClass<Any>(serviceInterface, fakePluginDescriptor)

                @Suppress("UNCHECKED_CAST")
                componentManager.registerServiceWithInterface(serviceInterfaceClass, serviceImplementationClass)
            } else {
                componentManager.registerService(serviceImplementationClass)
            }
        }
    }

    fun registerProjectListeners(project: MockProject, pluginRelativePath: String) {
        val pluginDescriptor = getOrCalculatePluginDescriptor(PluginDesignation(pluginRelativePath, project))
        val listenerDescriptors = pluginDescriptor.projectContainerDescriptor.listeners.orEmpty().ifEmpty {
            return
        }

        val listenersMap = ConcurrentHashMap<String, MutableList<ListenerDescriptor>>()
        for (listenerDescriptor in listenerDescriptors) {
            listenerDescriptor.pluginDescriptor = fakePluginDescriptor
            listenersMap.computeIfAbsent(listenerDescriptor.topicClassName) { SmartList() }.add(listenerDescriptor)
        }

        (project.analysisMessageBus as MessageBusEx).setLazyListeners(listenersMap)
    }

    private val MockComponentManager.classLoader
        get() = loadClass<Any>(PluginDesignation::class.java.name, fakePluginDescriptor).classLoader

    // workaround for ambiguity resolution
    private fun <T> MockComponentManager.registerServiceWithInterface(interfaceClass: Class<T>, implementationClass: Class<T>) {
        registerService(interfaceClass, implementationClass)
    }
}
