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
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.renderWithType
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
    reporter: ICReporter,
    alreadyCompiledSources: Set<File>
): LinkedHashSet<File> {
    val changesCollector = ChangesCollector()
    val globalSerializationBindings = JvmSerializationBindings()

    fun visitFirFiles(analyzedOutput: ModuleCompilerAnalyzedOutput) {
        val targetId: TargetId = run { //TODO(emazhukin) this feels like an abstraction level breakage
            val moduleName = phaseArtifact.configuration.moduleName ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME
            TargetId(moduleName, "java-production") // TODO: get rid of magic constant // is it even worth getting rid of?
        }

        for (file in analyzedOutput.fir) {
            println("fir runner diff - iteration: ${file.name}")
            file.accept(object : FirVisitor<Unit, MutableList<MetadataSerializer>>() {
                inline fun withMetadataSerializer(
                    metadata: FirMetadataSource,
                    data: MutableList<MetadataSerializer>,
                    body: (MetadataSerializer) -> Unit
                ) {
                    //println("fir runner diff - metadata: ${metadata.fir.symbol}")
                    val serializer = makeLocalFirMetadataSerializerForMetadataSource(
                        metadata,
                        analyzedOutput.session,
                        analyzedOutput.scopeSession,
                        globalSerializationBindings,
                        data.lastOrNull(),
                        targetId,
                        phaseArtifact.configuration,
                        actualizedExpectDeclarations = null //TODO(emazhukin) disable fir runner in kmp to skip a nasty new pile of test cases?
                    )
                    data.push(serializer)
                    body(serializer)
                    data.pop()
                }

                override fun visitElement(element: FirElement, data: MutableList<MetadataSerializer>) {
                    //println("fir runner diff - element: ${element.render()}")
                    element.acceptChildren(this, data)
                }

                override fun visitRegularClass(regularClass: FirRegularClass, data: MutableList<MetadataSerializer>) {
                    //println("fir runner diff - regular class: ${regularClass.name}")
                    visitClass(regularClass, data)
                }

                override fun visitAnonymousObject(anonymousObject: FirAnonymousObject, data: MutableList<MetadataSerializer>) {
                    //println("fir runner diff - anon object: ${anonymousObject.source}")
                    visitClass(anonymousObject, data)
                }

                override fun visitFile(file: FirFile, data: MutableList<MetadataSerializer>) {
                    //println("fir runner diff - file: ${file.name}")
                    val metadata = FirMetadataSource.File(file)
                    withMetadataSerializer(metadata, data) {
                        file.acceptChildren(this, data)
                        // TODO: compare package fragments?
                    }
                }

                override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: MutableList<MetadataSerializer>) {
                    //println("fir runner diff - simple fun: ${simpleFunction.name}")
                    data.firstOrNull()?.let { serializer ->
                        super.visitFunction(simpleFunction, data)
                        serializer.bindMethodMetadata(
                            FirMetadataSource.Function(simpleFunction),
                            Method(simpleFunction.name.asString(), simpleFunction.computeJvmDescriptor())
                        )
                    }
                }

                override fun visitConstructor(constructor: FirConstructor, data: MutableList<MetadataSerializer>) {
                    //println("fir runner diff - constructor: ${constructor.render()}")
                    super.visitConstructor(constructor, data)
                    data.first().bindMethodMetadata(
                        FirMetadataSource.Function(constructor),
                        Method(SpecialNames.INIT.asString(), constructor.computeJvmDescriptor(""))
                    )
                }

                override fun visitProperty(property: FirProperty, data: MutableList<MetadataSerializer>) {
                    //println("fir runner diff - property: ${property.name} ${property.source}")
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
                    //println("fir runner diff - klass: ${klass.render()}")
                    val metadata = FirMetadataSource.Class(klass)
                    withMetadataSerializer(metadata, data) { serializer ->
                        klass.acceptChildren(this, data)
                        serializer.serialize(metadata, FirMetadataSource.File(file))?.let { (classProto, nameTable) ->
                            //println("fir runner diff - if everything else fails, print class protos, but it requires extra tricks with DebugProto")
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

//    println(
//        "fir runner diff - all outputs: ${
//            analysisResults.outputs.joinToString(",") {
//                it.toString().replace("@.......".toRegex(), "")
//            }
//        }"
//    )
    for (output in phaseArtifact.result.outputs) {
        println("fir runner diff - output batch - ${output.session.kind}")
        visitFirFiles(output)
    }

    //////////////println("fir runner diff - input platform cache: ${listOf(caches.platformCache).joinToString(",")}")
    val (dirtyLookupSymbols, dirtyClassFqNames, forceRecompile) =
        changesCollector.getChangedAndImpactedSymbols(listOf(caches.platformCache), reporter)
    ////println("fir runner diff - collected so far: ${changesCollector.joinToString(",")}") // TODO either hack to make lists public, or add tostring there
    //println("fir runner diff - dirtyLookupSymbols: ${dirtyLookupSymbols.joinToString(",")}")
    //println("fir runner diff - dirtyClassFqNames: ${dirtyClassFqNames.joinToString(",")}")
    //println("fir runner diff - forceRecompile: ${forceRecompile.joinToString(",")}")

    // TODO(emazhukin) hey Dmitry, this is yet another version of "force to recompile" in IC :melting_face:
    val forceToRecompileFiles = mapClassesFqNamesToFiles(listOf(caches.platformCache), forceRecompile, reporter)
    //println("fir runner diff - forceToRecompileFiles: ${forceToRecompileFiles.joinToString(",")}")

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
        //println("fir runner diff - filtered: ${filter { !it.exists() }.joinToString(",")}")
        removeAll { !it.exists() }
    }
}
