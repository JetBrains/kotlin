/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.js.config.JSConfigurationKeys.INCREMENTAL_RESULTS_CONSUMER
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil
import org.jetbrains.kotlin.utils.JsMetadataVersion

// todo: think if possible to unify with org.jetbrains.kotlin.js.facade.K2JSTranslator.trySaveIncrementalData
internal fun trySaveIncrementalData(context: GeneratorContext, config: CompilerConfiguration, files: List<KtFile>) {
    val incrementalResults = config.get(INCREMENTAL_RESULTS_CONSUMER) ?: return

    val bindingContext = context.bindingContext
    val moduleDescriptor = context.moduleDescriptor

    val emptyByteArray = emptyArray<Byte>().toByteArray()

    for (file in files) {
        val memberScope = file.declarations.map { getDescriptorForElement(bindingContext, it) }
        val packagePart = serializeScope(bindingContext, moduleDescriptor, file.packageFqName, memberScope, config)
        val ioFile = VfsUtilCore.virtualToIoFile(file.virtualFile)
        incrementalResults.processPackagePart(ioFile, packagePart.toByteArray(), binaryAst = emptyByteArray, inlineData = emptyByteArray)
    }

    val settings = config.languageVersionSettings
    incrementalResults.processHeader(KotlinJavascriptSerializationUtil.serializeHeader(moduleDescriptor, null, settings).toByteArray())
}

// copied from org.jetbrains.kotlin.js.translate.utils.BindingUtils.getDescriptorForElement
fun getDescriptorForElement(
    context: BindingContext,
    element: PsiElement
): DeclarationDescriptor = BindingContextUtils.getNotNull(context, BindingContext.DECLARATION_TO_DESCRIPTOR, element)

// copied from org.jetbrains.kotlin.js.facade.K2JSTranslator.serializeScope
private fun serializeScope(
    bindingContext: BindingContext,
    moduleDescriptor: ModuleDescriptor,
    packageName: FqName,
    scope: Collection<DeclarationDescriptor>,
    configuration: CompilerConfiguration
): ProtoBuf.PackageFragment =
    KotlinJavascriptSerializationUtil.serializeDescriptors(
        bindingContext,
        moduleDescriptor,
        scope,
        packageName,
        configuration.languageVersionSettings,
        configuration.get(CommonConfigurationKeys.METADATA_VERSION) ?: JsMetadataVersion.INSTANCE
    )
