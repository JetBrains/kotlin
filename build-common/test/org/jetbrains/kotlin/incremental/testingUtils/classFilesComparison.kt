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

package org.jetbrains.kotlin.incremental.testingUtils

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.incremental.LocalFileKotlinClass
import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMapError
import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMapParser
import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMapSuccess
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.metadata.DebugProtoBuf
import org.jetbrains.kotlin.metadata.js.DebugJsProtoBuf
import org.jetbrains.kotlin.metadata.jvm.DebugJvmProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.BitEncoding
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.protobuf.ExtensionRegistry
import org.jetbrains.kotlin.serialization.js.JsSerializerProtocol
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadata
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.util.TraceClassVisitor
import org.junit.Assert
import org.junit.Assert.assertNotNull
import org.junit.ComparisonFailure
import java.io.*
import java.util.*
import java.util.zip.CRC32
import java.util.zip.GZIPInputStream

// Set this to true if you want to dump all bytecode (test will fail in this case)
private val DUMP_ALL = System.getProperty("comparison.dump.all") == "true"

fun assertEqualDirectories(expected: File, actual: File, forgiveExtraFiles: Boolean) {
    val pathsInExpected = getAllRelativePaths(expected)
    val pathsInActual = getAllRelativePaths(actual)

    val commonPaths = pathsInExpected.intersect(pathsInActual)
    val changedPaths = commonPaths
            .filter { DUMP_ALL || !Arrays.equals(File(expected, it).readBytes(), File(actual, it).readBytes()) }
            .sorted()

    val expectedString = getDirectoryString(expected, changedPaths)
    val actualString = getDirectoryString(actual, changedPaths)

    if (DUMP_ALL) {
        Assert.assertEquals(expectedString, actualString + " ")
    }

    if (forgiveExtraFiles) {
        // If compilation fails, output may be different for full rebuild and partial make. Parsing output (directory string) for simplicity.
        if (changedPaths.isEmpty()) {
            val expectedListingLines = expectedString.split('\n').toList()
            val actualListingLines = actualString.split('\n').toList()
            if (actualListingLines.containsAll(expectedListingLines)) {
                return
            }
        }
    }

    if (expectedString != actualString) {
        val message: String? = null
        throw ComparisonFailure(
            message,
            expectedString.replaceFirst(DIR_ROOT_PLACEHOLDER, expected.canonicalPath),
            actualString.replaceFirst(DIR_ROOT_PLACEHOLDER, actual.canonicalPath)
        )
    }

}

private fun File.checksumString(): String {
    val crc32 = CRC32()
    crc32.update(this.readBytes())
    return java.lang.Long.toHexString(crc32.value)
}

private const val DIR_ROOT_PLACEHOLDER = "<DIR_ROOT_PLACEHOLDER>"

private fun getDirectoryString(dir: File, interestingPaths: List<String>): String {
    val buf = StringBuilder()
    val p = Printer(buf)


    fun addDirContent(dir: File) {
        p.pushIndent()

        val listFiles = dir.listFiles()
        assertNotNull("$dir does not exist", listFiles)

        val children = listFiles!!.sortedWith(compareBy({ it.isDirectory }, { it.name }))
        for (child in children) {
            if (child.isDirectory) {
                if ((child.list()?.isNotEmpty() ?: false)) {
                    p.println(child.name)
                    addDirContent(child)
                }
            }
            else {
                p.println(child.name, " ", child.checksumString())
            }
        }

        p.popIndent()
    }


    p.println(DIR_ROOT_PLACEHOLDER)
    addDirContent(dir)

    for (path in interestingPaths) {
        p.println("================", path, "================")
        p.println(fileToStringRepresentation(File(dir, path)))
        p.println()
        p.println()
    }

    return buf.toString()
}

private fun getAllRelativePaths(dir: File): Set<String> {
    val result = HashSet<String>()
    FileUtil.processFilesRecursively(dir) {
        if (it!!.isFile) {
            result.add(FileUtil.getRelativePath(dir, it)!!)
        }

        true
    }

    return result
}

private fun classFileToString(classFile: File): String {
    val out = StringWriter()

    val traceVisitor = TraceClassVisitor(PrintWriter(out))
    ClassReader(classFile.readBytes()).accept(traceVisitor, 0)

    val classHeader = LocalFileKotlinClass.create(classFile, JvmMetadataVersion.INSTANCE)?.classHeader ?: return ""
    if (!classHeader.metadataVersion.isCompatibleWithCurrentCompilerVersion()) {
        error("Incompatible class ($classHeader): $classFile")
    }

    when (classHeader.kind) {
        KotlinClassHeader.Kind.FILE_FACADE, KotlinClassHeader.Kind.CLASS, KotlinClassHeader.Kind.MULTIFILE_CLASS_PART -> {
            ByteArrayInputStream(BitEncoding.decodeBytes(classHeader.data!!)).use { input ->
                out.write("\n------ string table types proto -----\n${DebugJvmProtoBuf.StringTableTypes.parseDelimitedFrom(input)}")

                when (classHeader.kind) {
                    KotlinClassHeader.Kind.FILE_FACADE ->
                        out.write("\n------ file facade proto -----\n${DebugProtoBuf.Package.parseFrom(input, getExtensionRegistry())}")
                    KotlinClassHeader.Kind.CLASS ->
                        out.write("\n------ class proto -----\n${DebugProtoBuf.Class.parseFrom(input, getExtensionRegistry())}")
                    KotlinClassHeader.Kind.MULTIFILE_CLASS_PART ->
                        out.write("\n------ multi-file part proto -----\n${DebugProtoBuf.Package.parseFrom(input, getExtensionRegistry())}")
                    else -> error(classHeader.kind)
                }
            }
        }
        KotlinClassHeader.Kind.MULTIFILE_CLASS -> {
            out.write("\n------ multi-file facade data -----\n")
            out.write(classHeader.data!!.joinToString("\n"))
        }
        KotlinClassHeader.Kind.SYNTHETIC_CLASS -> {
            // Synthetic class has no metadata, thus there can be no differences in it.
        }
        KotlinClassHeader.Kind.UNKNOWN -> error("Should not meet unknown classes here: $classFile")
    }

    return out.toString()
}

private fun metaJsToString(metaJsFile: File): String {
    val out = StringWriter()

    val metadataList = arrayListOf<KotlinJavascriptMetadata>()
    KotlinJavascriptMetadataUtils.parseMetadata(metaJsFile.readText(), metadataList)

    for (metadata in metadataList) {
        val (header, content) = GZIPInputStream(ByteArrayInputStream(metadata.body)).use { stream ->
            DebugJsProtoBuf.Header.parseDelimitedFrom(stream, JsSerializerProtocol.extensionRegistry) to
                    DebugJsProtoBuf.Library.parseFrom(stream, JsSerializerProtocol.extensionRegistry)
        }
        out.write("\n------ header -----\n$header")
        out.write("\n------ library -----\n$content")
    }

    return out.toString()
}

private fun kjsmToString(kjsmFile: File): String {
    val out = StringWriter()

    val stream = DataInputStream(kjsmFile.inputStream())
    // Read and skip the metadata version
    repeat(stream.readInt()) { stream.readInt() }

    val (header, content) =
            DebugJsProtoBuf.Header.parseDelimitedFrom(stream, JsSerializerProtocol.extensionRegistry) to
                    DebugJsProtoBuf.Library.parseFrom(stream, JsSerializerProtocol.extensionRegistry)

    out.write("\n------ header -----\n$header")
    out.write("\n------ library -----\n$content")

    return out.toString()
}

private fun sourceMapFileToString(sourceMapFile: File, generatedJsFile: File): String {
    val sourceMapParseResult = SourceMapParser.parse(sourceMapFile.readText())
    return when (sourceMapParseResult) {
        is SourceMapSuccess -> {
            val bytesOut = ByteArrayOutputStream()
            PrintStream(bytesOut).use { printStream ->
                sourceMapParseResult.value.debugVerbose(printStream, generatedJsFile)
            }
            bytesOut.toString()
        }
        is SourceMapError -> {
            sourceMapParseResult.message
        }
    }
}

private fun getExtensionRegistry(): ExtensionRegistry {
    val registry = ExtensionRegistry.newInstance()!!
    DebugJvmProtoBuf.registerAllExtensions(registry)
    return registry
}

private fun fileToStringRepresentation(file: File): String {
    return when {
        file.name.endsWith(".class") -> {
            classFileToString(file)
        }
        file.name.endsWith(KotlinJavascriptMetadataUtils.META_JS_SUFFIX) -> {
            metaJsToString(file)
        }
        file.name.endsWith(KotlinJavascriptSerializationUtil.CLASS_METADATA_FILE_EXTENSION) -> {
            kjsmToString(file)
        }
        file.name.endsWith(".js.map") -> {
            val generatedJsPath = file.canonicalPath.removeSuffix(".map")
            sourceMapFileToString(file, File(generatedJsPath))
        }
        else -> {
            file.readText()
        }
    }
}
