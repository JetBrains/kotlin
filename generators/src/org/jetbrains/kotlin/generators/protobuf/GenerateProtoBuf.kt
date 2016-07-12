/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.generators.protobuf

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.util.LineSeparator
import java.io.File
import java.util.regex.Pattern

// This file generates protobuf classes from formal description.
// To run it, you'll need protoc (protobuf compiler) 2.5.0 installed.
//
// * Windows: download and unpack from here: https://code.google.com/p/protobuf/downloads/list
// * Ubuntu: install "protobuf-compiler" package
// * Mac OS: install "protobuf" package from Homebrew or "protobuf-cpp" from MacPorts
// * You can also download source and build it yourself (https://code.google.com/p/protobuf/downloads/list)
//
// You may need to provide custom path to protoc executable, just modify this constant:
private const val PROTOC_EXE = "protoc"

class ProtoPath(val file: String) {
    val outPath: String = File(file).parent
    val packageName: String = findFirst(Pattern.compile("package (.+);"))
    val className: String = findFirst(Pattern.compile("option java_outer_classname = \"(.+)\";"))
    val debugClassName: String = "Debug$className"

    private fun findFirst(pattern: Pattern): String {
        for (line in File(file).readLines()) {
            val m = pattern.matcher(line)
            if (m.find()) return m.group(1)
        }
        error("Pattern not found in $file: $pattern")
    }
}

val PROTO_PATHS: List<ProtoPath> = listOf(
        ProtoPath("core/deserialization/src/descriptors.proto"),
        ProtoPath("core/deserialization/src/builtins.proto"),
        ProtoPath("js/js.serializer/src/js.proto"),
        ProtoPath("core/descriptor.loader.java/src/jvm_descriptors.proto"),
        ProtoPath("core/descriptor.loader.java/src/jvm_package_table.proto")
)

private val EXT_OPTIONS_PROTO_PATH = ProtoPath("core/deserialization/src/ext_options.proto")
private val PROTOBUF_PROTO_PATHS = listOf("./", "core/deserialization/src")

fun main(args: Array<String>) {
    try {
        checkVersion()

        modifyAndExecProtoc(EXT_OPTIONS_PROTO_PATH)

        for (protoPath in PROTO_PATHS) {
            execProtoc(protoPath.file, protoPath.outPath)
            modifyAndExecProtoc(protoPath)
        }

        println("Do not forget to run GenerateProtoBufCompare")
    }
    catch (e: Throwable) {
        e.printStackTrace()
        throw e
    }
    finally {
        // Workaround for JVM hanging: IDEA's process handler creates thread pool
        System.exit(0)
    }
}

private fun checkVersion() {
    val processOutput = ExecUtil.execAndGetOutput(GeneralCommandLine(PROTOC_EXE, "--version"))

    val version = processOutput.stdout.trim()
    if (version.isEmpty()) {
        throw AssertionError("Output is empty, stderr: ${processOutput.stderr}")
    }
    if (version != "libprotoc 2.5.0") {
        throw AssertionError("Expected protoc 2.5.0, but was: $version")
    }
}

private fun execProtoc(protoPath: String, outPath: String) {
    val commandLine = GeneralCommandLine(
            listOf(PROTOC_EXE, protoPath, "--java_out=$outPath") +
            PROTOBUF_PROTO_PATHS.map { "--proto_path=$it" }
    )
    println("running $commandLine")
    val processOutput = ExecUtil.execAndGetOutput(commandLine)
    print(processOutput.stdout)
    if (processOutput.stderr.isNotEmpty()) {
        throw AssertionError(processOutput.stderr)
    }

    renamePackages(protoPath, outPath)
}

private fun renamePackages(protoPath: String, outPath: String) {
    fun List<String>.findValue(regex: Regex): String? =
            mapNotNull { line ->
                regex.find(line)?.groupValues?.get(1)
            }.singleOrNull()

    val protoFileContents = File(protoPath).readLines()
    val packageName = protoFileContents.findValue("package ([\\w.]+);".toRegex())
                      ?: error("No package directive found in $protoPath")
    val className = protoFileContents.findValue("option java_outer_classname = \"(\\w+)\";".toRegex())
                    ?: error("No java_outer_classname option found in $protoPath")

    val javaFile = File(outPath, "${packageName.replace('.', '/')}/$className.java")
    if (!javaFile.exists()) {
        throw AssertionError("File does not exist: $javaFile")
    }

    javaFile.writeText(
            javaFile.readLines().map { line ->
                line.replace("com.google.protobuf", "org.jetbrains.kotlin.protobuf")
            }.joinToString(LineSeparator.getSystemLineSeparator().separatorString)
    )
}

private fun modifyAndExecProtoc(protoPath: ProtoPath) {
    val debugProtoFile = File(protoPath.file.replace(".proto", ".debug.proto"))
    debugProtoFile.writeText(modifyForDebug(protoPath))
    debugProtoFile.deleteOnExit()

    execProtoc(debugProtoFile.path, "build-common/test")
}

private fun modifyForDebug(protoPath: ProtoPath): String {
    var text = File(protoPath.file).readText()
            .replace("option java_outer_classname = \"${protoPath.className}\"",
                     "option java_outer_classname = \"${protoPath.debugClassName}\"") // give different name for class
            .replace("option optimize_for = LITE_RUNTIME;", "") // using default instead
    (listOf(EXT_OPTIONS_PROTO_PATH) + PROTO_PATHS).forEach {
        val file = it.file
        val newFile = file.replace(".proto", ".debug.proto")
        text = text.replace(file, newFile)
    }
    return text
}
