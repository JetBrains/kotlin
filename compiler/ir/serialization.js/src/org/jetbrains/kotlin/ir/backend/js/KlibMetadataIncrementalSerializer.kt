/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtPsiSourceFile
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.backend.common.serialization.metadata.FileVisitor
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataSerializer
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibSingleFileMetadataSerializer
import org.jetbrains.kotlin.backend.common.serialization.toIoFileOrNull
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.util.klibMetadataVersionOrDefault
import java.io.File

// TODO: need a refactoring between IncrementalSerializer and MonolithicSerializer.
class KlibMetadataIncrementalSerializer(
    private val ktFiles: List<KtFile>,
    private val bindingContext: BindingContext,
    private val moduleDescriptor: ModuleDescriptor,
    languageVersionSettings: LanguageVersionSettings,
    metadataVersion: MetadataVersion,
    project: Project,
    exportKDoc: Boolean
) : KlibMetadataSerializer(
    languageVersionSettings = languageVersionSettings,
    metadataVersion = metadataVersion,
    project = project,
    exportKDoc = exportKDoc,
    skipExpects = true // Incremental compilation is not supposed to work when producing pure metadata (IR-less) KLIBs.
), KlibSingleFileMetadataSerializer<KtFile> {

    constructor(
        files: List<KtFile>,
        configuration: CompilerConfiguration,
        project: Project,
        bindingContext: BindingContext,
        moduleDescriptor: ModuleDescriptor,
    ) : this(
        ktFiles = files,
        bindingContext = bindingContext,
        moduleDescriptor = moduleDescriptor,
        languageVersionSettings = configuration.languageVersionSettings,
        metadataVersion = configuration.klibMetadataVersionOrDefault(),
        project = project,
        exportKDoc = false,
    )

    override fun serializeSingleFileMetadata(file: KtFile): ProtoBuf.PackageFragment {
        val memberScope = file.declarations.map { getDescriptorForElement(bindingContext, it) }
        return serializePackageFragment(moduleDescriptor, memberScope, file.packageFqName)
    }

    override val sourceFiles: Set<File> by lazy(LazyThreadSafetyMode.NONE) {
        ktFiles.mapTo(mutableSetOf()) { KtPsiSourceFile(it).toIoFileOrNull()!! }
    }

    override fun forEachFile(block: FileVisitor<KtFile>) {
        ktFiles.forEachIndexed { i, ktFile ->
            val psiSourceFile = KtPsiSourceFile(ktFile)
            block.visit(i, psiSourceFile.toIoFileOrNull()!!, ktFile, psiSourceFile, ktFile.packageFqName)
        }
    }

    private fun getDescriptorForElement(context: BindingContext, element: PsiElement): DeclarationDescriptor =
        BindingContextUtils.getNotNull(context, BindingContext.DECLARATION_TO_DESCRIPTOR, element)

    private fun serializePackageFragment(
        module: ModuleDescriptor,
        scope: Collection<DeclarationDescriptor>,
        fqName: FqName
    ): ProtoBuf.PackageFragment {

        val allDescriptors = scope.filter {
            it.module == module
        }

        val classifierDescriptors = allDescriptors
            .filterIsInstance<ClassifierDescriptor>()
            .sortedBy { it.fqNameSafe.asString() }

        val topLevelDescriptors = DescriptorSerializer.sort(
            allDescriptors
                .filterIsInstance<CallableDescriptor>()
        )

        // TODO: For now, in the incremental serializer, we assume
        // there is only a single package fragment per file.
        // This is no always the case, actually.
        // But marrying split package fragments with incremental compilation is an endeavour.
        // See monolithic serializer for details.
        return serializeDescriptors(fqName, classifierDescriptors, topLevelDescriptors).single()
    }

    // TODO: For now, in the incremental serializer, we assume
    // there is only a single package fragment per file.
    // This is no always the case, actually.
    // But marrying split package fragments with incremental compilation is an endeavour.
    // See monolithic serializer for details.
    override val TOP_LEVEL_DECLARATION_COUNT_PER_FILE = null
    override val TOP_LEVEL_CLASS_DECLARATION_COUNT_PER_FILE = null
}
