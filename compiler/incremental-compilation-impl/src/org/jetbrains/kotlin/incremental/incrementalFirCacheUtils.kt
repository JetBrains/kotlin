/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.jvm.metadata.MetadataSerializer
import org.jetbrains.kotlin.build.report.ICReporter
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFrontendPipelineArtifact
import org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings
import org.jetbrains.kotlin.config.moduleName
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.backend.jvm.makeLocalFirMetadataSerializerForMetadataSource
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.pipeline.ModuleCompilerAnalyzedOutput
import org.jetbrains.kotlin.fir.scopes.jvm.computeJvmDescriptor
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.org.objectweb.asm.commons.Method
import java.io.File

internal fun collectNewDirtySourcesFromFirResult(
    phaseArtifact: JvmFrontendPipelineArtifact,
    caches: IncrementalJvmCachesManager,
    alreadyCompiledSources: Set<File>,
    reporter: ICReporter,
): LinkedHashSet<File> {
    val changesCollector = ChangesCollector()
    val globalSerializationBindings = JvmSerializationBindings()

    fun visitFirFiles(analyzedOutput: ModuleCompilerAnalyzedOutput) {
        val targetId: TargetId = run { //TODO(emazhukin) this feels like an abstraction level breakage
            val moduleName = phaseArtifact.configuration.moduleName ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME
            TargetId(moduleName, "java-production") // TODO: get rid of magic constant // is it even worth getting rid of?
        }

        for (file in analyzedOutput.fir) {
            file.accept(object : FirVisitor<Unit, MutableList<MetadataSerializer>>() {
                inline fun withMetadataSerializer(
                    metadata: FirMetadataSource,
                    data: MutableList<MetadataSerializer>,
                    body: (MetadataSerializer) -> Unit
                ) {
                    val serializer = makeLocalFirMetadataSerializerForMetadataSource(
                        metadata,
                        analyzedOutput.session,
                        analyzedOutput.scopeSession,
                        globalSerializationBindings,
                        data.lastOrNull(),
                        targetId,
                        phaseArtifact.configuration,
                        actualizedExpectDeclarations = null
                    )
                    data.push(serializer)
                    body(serializer)
                    data.pop()
                }

                override fun visitElement(element: FirElement, data: MutableList<MetadataSerializer>) {
                    element.acceptChildren(this, data)
                }

                override fun visitRegularClass(regularClass: FirRegularClass, data: MutableList<MetadataSerializer>) {
                    visitClass(regularClass, data)
                }

                override fun visitAnonymousObject(anonymousObject: FirAnonymousObject, data: MutableList<MetadataSerializer>) {
                    visitClass(anonymousObject, data)
                }

                override fun visitFile(file: FirFile, data: MutableList<MetadataSerializer>) {
                    val metadata = FirMetadataSource.File(file)
                    withMetadataSerializer(metadata, data) {
                        file.acceptChildren(this, data)
                        // TODO: compare package fragments?
                    }
                }

                override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: MutableList<MetadataSerializer>) {
                    data.firstOrNull()?.let { serializer ->
                        super.visitFunction(simpleFunction, data)
                        serializer.bindMethodMetadata(
                            FirMetadataSource.Function(simpleFunction),
                            Method(simpleFunction.name.asString(), simpleFunction.computeJvmDescriptor())
                        )
                    }
                }

                override fun visitConstructor(constructor: FirConstructor, data: MutableList<MetadataSerializer>) {
                    super.visitConstructor(constructor, data)
                    data.first().bindMethodMetadata(
                        FirMetadataSource.Function(constructor),
                        Method(SpecialNames.INIT.asString(), constructor.computeJvmDescriptor(""))
                    )
                }

                override fun visitProperty(property: FirProperty, data: MutableList<MetadataSerializer>) {
                    property.acceptChildren(this, data)
                    //                    data.firstOrNull()?.let {
                    //                        property.acceptChildren(this, data)
                    //                        it.bindPropertyMetadata(
                    //                            FirMetadataSource.Property(property),
                    //                            Method(property.name.asString(), ""),//property.computeJvmDescriptor())
                    //                            IrDeclarationOrigin.DEFINED
                    //                        )
                    //                    }
                }

                override fun visitClass(klass: FirClass, data: MutableList<MetadataSerializer>) {
                    val metadata = FirMetadataSource.Class(klass)
                    withMetadataSerializer(metadata, data) { serializer ->
                        klass.acceptChildren(this, data)
                        serializer.serialize(metadata, FirMetadataSource.File(file))?.let { (classProto, nameTable) ->
                            caches.platformCache.saveFrontendClassToCache(
                                klass.classId,
                                classProto as ProtoBuf.Class,
                                nameTable,
                                null, // TODO: !!
                                changesCollector
                            )
                        }
                    }
                }
            }, mutableListOf())
        }
    }

    for (output in phaseArtifact.result.outputs) {
        visitFirFiles(output)
    }

    val (dirtyLookupSymbols, dirtyClassFqNames, forceRecompile) =
        changesCollector.getChangedAndImpactedSymbols(listOf(caches.platformCache), reporter)

    // TODO(emazhukin) hey Dmitry, this is yet another version of "force to recompile" in IC :melting_face:
    val forceToRecompileFiles = mapClassesFqNamesToFiles(listOf(caches.platformCache), forceRecompile, reporter)

    return linkedSetOf<File>().apply {
        addAll(mapLookupSymbolsToFiles(caches.lookupCache, dirtyLookupSymbols, reporter, excludes = alreadyCompiledSources))
        addAll(
            mapClassesFqNamesToFiles(
                listOf(caches.platformCache),
                dirtyClassFqNames,
                reporter,
                excludes = alreadyCompiledSources
            )
        )
        if (!alreadyCompiledSources.containsAll(forceToRecompileFiles)) {
            addAll(forceToRecompileFiles)
        }
        removeAll { !it.exists() }
    }
}
