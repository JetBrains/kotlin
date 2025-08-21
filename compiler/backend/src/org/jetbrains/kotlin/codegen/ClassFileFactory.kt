/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.codegen

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.backend.common.output.OutputFile
import org.jetbrains.kotlin.backend.common.output.OutputFileCollection
import org.jetbrains.kotlin.codegen.extensions.ClassFileFactoryFinalizerExtension
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.JvmAnalysisFlags.strictMetadataVersionSemantics
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.load.kotlin.loadModuleMapping
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.metadata.jvm.JvmModuleProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMapping
import org.jetbrains.kotlin.metadata.jvm.deserialization.serializeToByteArray
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.org.objectweb.asm.Type
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.*

class ClassFileFactory(
    val generationState: GenerationState,
    private val builderFactory: ClassBuilderFactory,
    private val finalizers: List<ClassFileFactoryFinalizerExtension>
) : OutputFileCollection {
    private val generators: MutableMap<String, OutAndSourceFileList> = Collections.synchronizedMap(LinkedHashMap())
    private val generatedForCompilerPluginOutputs: MutableSet<String> = LinkedHashSet()

    private var isDone = false

    private val sourceFiles: MutableSet<File> = HashSet()
    val packagePartRegistry: PackagePartRegistry = PackagePartRegistry()

    fun newVisitor(origin: JvmDeclarationOrigin, asmType: Type, sourceFiles: List<File>): ClassBuilder {
        val answer = builderFactory.newClassBuilder(origin)
        val classFileName = asmType.internalName + ".class"
        generators.put(
            classFileName,
            ClassBuilderAndSourceFileList(answer, sourceFiles)
        )
        if (origin.generatedForCompilerPlugin) {
            generatedForCompilerPluginOutputs.add(classFileName)
        }
        return answer
    }

    fun done() {
        if (!isDone) {
            isDone = true
            for (extension in finalizers) {
                extension.finalizeClassFactory(this)
            }
        }
    }

    fun addSerializedBuiltinsPackageMetadata(path: String, serialized: ByteArray) {
        generators.put(path, object : OutAndSourceFileList(sourceFiles.toList()) {
            override fun asBytes(factory: ClassBuilderFactory): ByteArray {
                return serialized
            }

            override fun asText(factory: ClassBuilderFactory): String {
                throw UnsupportedOperationException("No string representation for protobuf-serialized metadata")
            }
        })
    }

    fun setModuleMapping(moduleProto: JvmModuleProtoBuf.Module) {
        generators.put(
            JvmCodegenUtil.getMappingFileName(generationState.moduleName),
            object : OutAndSourceFileList(sourceFiles.toList()) {
                override fun asBytes(factory: ClassBuilderFactory): ByteArray {
                    var flags = 0
                    if (generationState.config.languageVersionSettings.getFlag(strictMetadataVersionSemantics)) {
                        flags = flags or ModuleMapping.STRICT_METADATA_VERSION_SEMANTICS_FLAG
                    }
                    return moduleProto.serializeToByteArray(metadataVersionToUseForModuleMapping, flags)
                }

                override fun asText(factory: ClassBuilderFactory): String {
                    return String(asBytes(factory), StandardCharsets.UTF_8)
                }
            })
    }

    private val metadataVersionToUseForModuleMapping: BinaryVersion
        get() {
            val version = generationState.config.metadataVersion
            if (version.major == LanguageVersion.KOTLIN_2_0.major &&
                version.minor == LanguageVersion.KOTLIN_2_0.minor
            ) {
                // If language version is >= 2.0, we're using metadata version 1.9.*. This is needed because before Kotlin 1.8.20-RC, there was
                // a bug in determining whether module metadata is written in the pre-1.4 format, or in the 1.4+ format with an extra integer
                // for module-wide flags (see https://github.com/jetbrains/kotlin/commit/25c600c556a5).
                //
                // Normally it should not be possible to suffer from it because we have only one version forward compatibility on JVM. However,
                // with `-Xskip-metadata-version-check`, which is used in Gradle, pre-1.8.20-RC Kotlin compilers were trying to read the 2.0
                // module metadata in the wrong format and failed with an exception: KT-62531.
                //
                // Since module metadata is not supposed to have any changes in 2.0, we're using the metadata version 1.9 as a workaround. This
                // way, it's still written in the 1.4+ format, and old compilers will correctly understand that it's written in the 1.4+ format.
                //
                // Patch version does not affect anything, so we can use any number, for example 9999 to make it more recognizable that it's
                // not a real Kotlin version, and rather a substitute for the 2.0 metadata version.
                //
                // This workaround can be removed once we no longer support language version 2.0.
                return MetadataVersion(1, 9, 9999)
            }
            return version
        }

    override fun asList(): List<OutputFile> {
        done()
        return this.currentOutput
    }

    val currentOutput: List<OutputFile>
        get() = generators.keys.map { OutputClassFile(relativePath = it, generatedForCompilerPlugin = it in generatedForCompilerPluginOutputs) }

    override fun get(relativePath: String): OutputFile? {
        return runIf(generators.containsKey(relativePath)) {
            OutputClassFile(relativePath, generatedForCompilerPlugin = relativePath in generatedForCompilerPluginOutputs)
        }
    }

    @TestOnly
    fun createText(): String {
        return createText(ignorePrefixPath = null)
    }

    private class ModuleMappingException(message: String?) : RuntimeException(message)

    @TestOnly
    fun createText(ignorePrefixPath: String?): String {
        // NB this method is frequently used in JVM BE tests to display generated bytecode in case of test failure.
        // It should be permissive, and should try to handle exceptions gracefully (otherwise you would make JVM BE devs unhappy).

        val answer = StringBuilder()

        for (file in asList()) {
            if (ignorePrefixPath != null && file.relativePath.startsWith(ignorePrefixPath)) continue
            val relativePath = File(file.relativePath)
            answer.append("@").append(relativePath).append('\n')
            when (relativePath.extension) {
                "class" -> answer.append(file.asText())
                "kotlin_module" -> try {
                    val mapping = ModuleMapping.Companion.loadModuleMapping(
                        file.asByteArray(), relativePath.path,
                        DeserializationConfiguration.Default
                    ) { version: MetadataVersion? ->
                        throw ModuleMappingException("Generated module has incompatible JVM metadata version: $version")
                    }
                    for (entry in mapping.packageFqName2Parts.entries) {
                        val packageFqName = FqName(entry.key)
                        val packageParts = entry.value
                        answer.append("<package ").append(packageFqName).append(": ").append(packageParts.parts).append(">\n")
                    }
                } catch (e: ModuleMappingException) {
                    answer.append(relativePath).append(": ").append(e.message).append("\n")
                }
                else -> answer.append("Unknown output file: ").append(file)
            }
        }

        return answer.toString()
    }

    @TestOnly
    fun createTextForEachFile(): Map<String, String> {
        val answer: MutableMap<String, String> = LinkedHashMap()
        for (file in asList()) {
            answer.put(file.relativePath, file.asText())
        }
        return answer
    }

    fun registerSourceFiles(files: Collection<File?>) {
        for (file in files) {
            // We ignore non-physical files here, because this code is needed to tell the make what inputs affect which outputs
            // a non-physical file cannot be processed by make
            if (file == null) continue
            sourceFiles.add(file)
        }
    }

    private inner class OutputClassFile(
        override val relativePath: String,
        override val generatedForCompilerPlugin: Boolean,
    ) : OutputFile {
        override val sourceFiles: List<File>
            get() {
                val pair: OutAndSourceFileList = generators[this.relativePath]!!
                checkNotNull(pair) { "No record for binary file " + this.relativePath }

                return pair.sourceFiles
            }

        override fun asByteArray(): ByteArray {
            try {
                return generators[this.relativePath]!!.asBytes(builderFactory)
            } catch (e: RuntimeException) {
                throw RuntimeException("Error generating class file " + this.toString() + ": " + e.message, e)
            }
        }

        override fun asText(): String {
            try {
                return generators[this.relativePath]!!.asText(builderFactory)
            } catch (e: RuntimeException) {
                throw RuntimeException("Error generating class file $this: ${e.message}", e)
            }
        }

        override fun toString(): String {
            return "$relativePath (compiled from $sourceFiles)"
        }
    }

    private class ClassBuilderAndSourceFileList(
        private val classBuilder: ClassBuilder,
        sourceFiles: List<File>,
    ) : OutAndSourceFileList(sourceFiles) {
        override fun asBytes(factory: ClassBuilderFactory): ByteArray {
            synchronized(this) {
                return factory.asBytes(classBuilder)
            }
        }

        override fun asText(factory: ClassBuilderFactory): String {
            return factory.asText(classBuilder)
        }
    }

    private abstract class OutAndSourceFileList(val sourceFiles: List<File>) {
        abstract fun asBytes(factory: ClassBuilderFactory): ByteArray

        abstract fun asText(factory: ClassBuilderFactory): String
    }

    fun removeClasses(classNamesToRemove: Set<String?>) {
        for (classInternalName in classNamesToRemove) {
            generators.remove("$classInternalName.class")
        }
    }
}
