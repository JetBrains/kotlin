/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.builtins

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.metadata.FirLegacyMetadataSerializer
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.pipeline.ModuleCompilerAnalyzedOutput
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCloneableSymbolProvider
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import java.io.File

internal class FirBuiltInsSerializer(
    configuration: CompilerConfiguration,
    environment: KotlinCoreEnvironment,
) : FirLegacyMetadataSerializer(configuration, environment) {
    override fun serialize(analysisResult: List<ModuleCompilerAnalyzedOutput>, destDir: File): OutputInfo? {
        val (session, scopeSession, firFiles) = analysisResult.single()
        destDir.deleteRecursively()
        if (!destDir.mkdirs()) {
            error("Could not make directories: $destDir")
        }

        val contentPerPackage = collectPackagesContent(firFiles)
        @OptIn(SymbolInternals::class)
        contentPerPackage.getOrPut(StandardNames.BUILT_INS_PACKAGE_FQ_NAME) { PackageContent() }.classes +=
            FirCloneableSymbolProvider(session, session.moduleData, session.kotlinScopeProvider)
                .getClassLikeSymbolByClassId(StandardClassIds.Cloneable)!!.fir as FirRegularClass

        for ((packageFqName, content) in contentPerPackage) {
            val destFile = File(destDir, BuiltInSerializerProtocol.getBuiltInsFilePath(packageFqName))
            val serializer = PackageSerializer(
                packageFqName, content.classes, content.membersPerFile.values.flatten(),
                destFile, session, scopeSession, BuiltInsBinaryVersion.INSTANCE,
            )
            serializer.serialize()
        }

        return OutputInfo(totalSize, totalFiles)
    }
}