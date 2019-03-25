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

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProviderImpl
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.libraries.Library
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.resolve.JvmTarget
import org.jetbrains.kotlin.idea.compiler.configuration.Kotlin2JvmCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.facet.getOrCreateFacet
import org.jetbrains.kotlin.idea.facet.initializeIfNeeded
import org.jetbrains.kotlin.idea.framework.JavaRuntimeDetectionUtil
import org.jetbrains.kotlin.idea.framework.JavaRuntimeLibraryDescription
import org.jetbrains.kotlin.idea.framework.JsLibraryStdDetectionUtil
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.kotlin.idea.util.projectStructure.sdk
import org.jetbrains.kotlin.idea.util.projectStructure.version
import org.jetbrains.kotlin.idea.versions.LibraryJarDescriptor
import org.jetbrains.kotlin.platform.impl.JvmIdePlatformKind
import org.jetbrains.kotlin.resolve.DefaultBuiltInPlatforms
import org.jetbrains.kotlin.resolve.TargetPlatform

open class KotlinJavaModuleConfigurator protected constructor() : KotlinWithLibraryConfigurator() {
    override fun isApplicable(module: Module): Boolean {
        return super.isApplicable(module) && !hasBrokenJsRuntime(module)
    }

    override fun isConfigured(module: Module): Boolean {
        return hasKotlinJvmRuntimeInScope(module)
    }

    override val libraryName: String
        get() = JavaRuntimeLibraryDescription.LIBRARY_NAME

    override val dialogTitle: String
        get() = JavaRuntimeLibraryDescription.DIALOG_TITLE

    override val libraryCaption: String
        get() = JavaRuntimeLibraryDescription.LIBRARY_CAPTION

    override val messageForOverrideDialog: String
        get() = JavaRuntimeLibraryDescription.JAVA_RUNTIME_LIBRARY_CREATION

    override val presentableText: String
        get() = "Java"

    override val name: String
        get() = NAME

    override val targetPlatform: TargetPlatform
        get() = DefaultBuiltInPlatforms.jvmPlatform

    override fun getLibraryJarDescriptors(sdk: Sdk?): List<LibraryJarDescriptor> {
        var result = listOf(
            LibraryJarDescriptor.RUNTIME_JAR,
            LibraryJarDescriptor.RUNTIME_SRC_JAR,
            LibraryJarDescriptor.REFLECT_JAR,
            LibraryJarDescriptor.REFLECT_SRC_JAR,
            LibraryJarDescriptor.TEST_JAR,
            LibraryJarDescriptor.TEST_SRC_JAR
        )
        val sdkVersion = sdk?.version ?: return result
        if (sdkVersion.isAtLeast(JavaSdkVersion.JDK_1_7)) {
            result += listOf(LibraryJarDescriptor.RUNTIME_JDK7_JAR, LibraryJarDescriptor.RUNTIME_JDK7_SOURCES_JAR)
        }
        if (sdkVersion.isAtLeast(JavaSdkVersion.JDK_1_8)) {
            result += listOf(LibraryJarDescriptor.RUNTIME_JDK8_JAR, LibraryJarDescriptor.RUNTIME_JDK8_SOURCES_JAR)
        }

        return result
    }

    override val libraryMatcher: (Library, Project) -> Boolean =
        { library, _ -> JavaRuntimeDetectionUtil.getRuntimeJar(library.getFiles(OrderRootType.CLASSES).asList()) != null }

    override fun configureKotlinSettings(modules: List<Module>) {
        val project = modules.firstOrNull()?.project ?: return
        val canChangeProjectSettings = project.allModules().all {
            it.sdk?.version?.isAtLeast(JavaSdkVersion.JDK_1_8) ?: true
        }
        if (canChangeProjectSettings) {
            Kotlin2JvmCompilerArgumentsHolder.getInstance(project).update {
                jvmTarget = "1.8"
            }
        } else {
            for (module in modules) {
                val sdkVersion = module.sdk?.version
                if (sdkVersion != null && sdkVersion.isAtLeast(JavaSdkVersion.JDK_1_8)) {
                    val modelsProvider = IdeModifiableModelsProviderImpl(project)
                    try {
                        val facet = module.getOrCreateFacet(modelsProvider, useProjectSettings = false, commitModel = true)
                        val facetSettings = facet.configuration.settings
                        facetSettings.initializeIfNeeded(module, null, DefaultBuiltInPlatforms.jvm18)
                        (facetSettings.compilerArguments as? K2JVMCompilerArguments)?.jvmTarget = "1.8"
                    } finally {
                        modelsProvider.dispose()
                    }
                }
            }
        }
    }

    override fun configureModule(
        module: Module,
        classesPath: String,
        sourcesPath: String,
        collector: NotificationMessageCollector,
        forceJarState: FileState?,
        useBundled: Boolean
    ) {
        super.configureModule(module, classesPath, sourcesPath, collector, forceJarState, useBundled)
        addStdlibToJavaModuleInfo(module, collector)
    }

    companion object {
        const val NAME = "java"

        val instance: KotlinJavaModuleConfigurator
            get() = Extensions.findExtension(KotlinProjectConfigurator.EP_NAME, KotlinJavaModuleConfigurator::class.java)
    }

    private fun hasBrokenJsRuntime(module: Module): Boolean {
        for (orderEntry in ModuleRootManager.getInstance(module).orderEntries) {
            val library = (orderEntry as? LibraryOrderEntry)?.library as? LibraryEx ?: continue
            if (JsLibraryStdDetectionUtil.hasJsStdlibJar(library, module.project, ignoreKind = true)) return true
        }
        return false
    }
}
