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

import com.intellij.execution.util.ExecUtil
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
val PROTOC_EXE = "protoc"

public data class ProtoPath(
        public val file: String,
        public val outPath: String
) {
    public val packageName: String = findFirst(Pattern.compile("package (.+);"))
    public val className: String = findFirst(Pattern.compile("option java_outer_classname = \"(.+)\";"))
    public val debugClassName: String = "Debug$className"

    private fun findFirst(pattern: Pattern): String {
        for (line in File(file).readLines()) {
            val m = pattern.matcher(line)
            if (m.find()) return m.group(1)
        }
        error("Pattern not found in $file: $pattern")
    }
}

public val PROTO_PATHS: List<ProtoPath> = listOf(
        ProtoPath("core/serialization/src/descriptors.proto", "core/serialization/src"),
        ProtoPath("core/serialization/src/builtins.proto", "core/serialization/src"),
        ProtoPath("core/serialization.js/src/js.proto", "core/serialization.js/src"),
        ProtoPath("core/serialization.jvm/src/jvm_descriptors.proto", "core/serialization.jvm/src")
)

fun main(args: Array<String>) {
    try {
        checkVersion()

        for (protoPath in PROTO_PATHS) {
            execProtoc(protoPath.file, protoPath.outPath)
            modifyAndExecProtoc(protoPath)
        }
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

fun checkVersion() {
    val processOutput = ExecUtil.execAndGetOutput(listOf(PROTOC_EXE, "--version"), null)

    val version = processOutput.getStdout().trim()
    if (version.isEmpty()) {
        throw AssertionError("Output is empty, stderr: " + processOutput.getStderr())
    }
    if (version != "libprotoc 2.5.0") {
        throw AssertionError("Expected protoc 2.5.0, but was: " + version)
    }
}

fun execProtoc(protoPath: String, outPath: String) {
    val processOutput = ExecUtil.execAndGetOutput(listOf(PROTOC_EXE, protoPath, "--java_out=$outPath"), null)
    print(processOutput.getStdout())
    if (processOutput.getStderr().isNotEmpty()) {
        throw AssertionError(processOutput.getStderr())
    }
}

fun modifyAndExecProtoc(protoPath: ProtoPath) {
    val debugProtoFile = File(protoPath.file.replace(".proto", ".debug.proto"))
    debugProtoFile.writeText(modifyForDebug(protoPath))
    debugProtoFile.deleteOnExit()

    execProtoc(debugProtoFile.getPath(), "compiler/tests")
}

fun modifyForDebug(protoPath: ProtoPath): String {
    return File(protoPath.file).readText()
            .replace("option java_outer_classname = \"${protoPath.className}\"",
                     "option java_outer_classname = \"${protoPath.debugClassName}\"") // give different name for class
            .replace("option optimize_for = LITE_RUNTIME;", "") // using default instead
            .replace(".proto\"", ".debug.proto\"") // for "import" statement in proto
}
