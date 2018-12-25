/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.builtins.FunctionInterfacePackageFragment
import org.jetbrains.kotlin.builtins.KOTLIN_REFLECT_FQ_NAME
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.builtins.functions.BuiltInFictitiousFunctionClassFactory
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.context.MutableModuleContextImpl
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.deserialization.ClassDescriptorFactory
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.frontend.js.di.createTopDownAnalyzerForJs
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.js.analyzer.JsAnalysisResult
import org.jetbrains.kotlin.js.resolve.MODULE_KIND
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.Printer

fun analyzeFiles(
    files: Collection<KtFile>,
    project: Project,
    configuration: CompilerConfiguration,
    moduleDescriptors: List<ModuleDescriptorImpl>,
    friendModuleDescriptors: List<ModuleDescriptorImpl>,
    optionalBuiltInsModule: ModuleDescriptorImpl? // null if this module is builtins module
): JsAnalysisResult {
    val moduleName = configuration[CommonConfigurationKeys.MODULE_NAME]!!
    val projectContext = ProjectContext(project)

    val builtIns = object : KotlinBuiltIns(projectContext.storageManager) {}

    val module = ModuleDescriptorImpl(Name.special("<$moduleName>"), projectContext.storageManager, builtIns, null)

    val builtInsModule: ModuleDescriptorImpl = optionalBuiltInsModule ?: module
    builtIns.builtInsModule = builtInsModule

    val context = MutableModuleContextImpl(module, projectContext)

    context.module.setDependencies(
        listOf(context.module) +
                moduleDescriptors,
        friendModuleDescriptors.toSet()
    )

    val moduleKind = ModuleKind.PLAIN

    val trace = BindingTraceContext()
    trace.record(MODULE_KIND, context.module, moduleKind)
    return analyzeFilesWithGivenTrace(files, trace, context, configuration)
}

fun analyzeFilesWithGivenTrace(
    files: Collection<KtFile>,
    trace: BindingTrace,
    moduleContext: ModuleContext,
    configuration: CompilerConfiguration
): JsAnalysisResult {
    val lookupTracker = configuration.get(CommonConfigurationKeys.LOOKUP_TRACKER) ?: LookupTracker.DO_NOTHING
    val expectActualTracker = configuration.get(CommonConfigurationKeys.EXPECT_ACTUAL_TRACKER) ?: ExpectActualTracker.DoNothing
    val languageVersionSettings = configuration.languageVersionSettings

    val classFactory = BuiltInFictitiousFunctionClassFactory(moduleContext.storageManager, moduleContext.module)
    val packageFragment = FunctionInterfacePackageFragmentProvider(classFactory, moduleContext.module)

    val analyzerForJs = createTopDownAnalyzerForJs(
        moduleContext, trace,
        FileBasedDeclarationProviderFactory(moduleContext.storageManager, files),
        languageVersionSettings,
        lookupTracker,
        expectActualTracker,
        packageFragment
    )
    analyzerForJs.analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, files)
    return JsAnalysisResult.success(trace, moduleContext.module)
}