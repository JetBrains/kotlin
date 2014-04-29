/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.generators.protobuf

import com.intellij.execution.util.ExecUtil
import com.intellij.execution.process.BaseOSProcessHandler
import java.util.concurrent.ExecutorService
import java.io.File

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

fun main(args: Array<String>) {
    try {
        checkVersion()

        val commonProto = "core/serialization/src/descriptors.proto"
        val javaProto = "core/serialization.java/src/java_descriptors.proto"

        execProtoc(commonProto, "core/serialization/src")
        execProtoc(javaProto, "core/serialization.java/src")

        modifyAndExecProtoc(commonProto, "compiler/tests")
        modifyAndExecProtoc(javaProto, "compiler/tests")
    }
    finally {
        // Workaround for JVM hanging: IDEA's process handler creates thread pool
        System.exit(0)
    }
}

fun checkVersion() {
    val processOutput = ExecUtil.execAndGetOutput(listOf(PROTOC_EXE, "--version"), null)

    val version = processOutput.getStdout()!!.trim()
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

fun modifyAndExecProtoc(protoPath: String, outPath: String) {
    val originalText = File(protoPath).readText()

    val debugProtoFile = File(protoPath.replace(".proto", ".debug.proto"))
    debugProtoFile.writeText(modifyForDebug(originalText))
    debugProtoFile.deleteOnExit()

    execProtoc(debugProtoFile.getPath(), outPath)
}

fun modifyForDebug(originalProto: String): String {
    return originalProto
            .replace("java_outer_classname = \"", "java_outer_classname = \"Debug") // give different name for class
            .replace("option optimize_for = LITE_RUNTIME;", "") // using default instead
            .replace(".proto\"", ".debug.proto\"") // for "import" statement in proto
}

