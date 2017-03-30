/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.native.interop.gen.jvm

import org.jetbrains.kotlin.native.interop.indexer.Language
import org.jetbrains.kotlin.native.interop.indexer.NativeIndex
import org.jetbrains.kotlin.native.interop.indexer.NativeLibrary
import org.jetbrains.kotlin.native.interop.indexer.buildNativeIndex
import java.io.File
import java.lang.IllegalArgumentException
import java.util.*
import kotlin.reflect.KFunction

fun main(args: Array<String>) {
    val konanHome = System.getProperty("konan.home")!!

    // TODO: remove OSX defaults.
    val substitutions = mapOf(
            "arch" to (System.getenv("TARGET_ARCH") ?: "x86-64"),
            "os" to (System.getenv("TARGET_OS") ?: detectHost())
    )

    processLib(konanHome, substitutions, args.asList())
}

private fun detectHost(): String {
    val os = System.getProperty("os.name")
    when (os) {
        "Linux" -> return "linux"
        "Windows" -> return "win"
        "Mac OS X" -> return "osx"
        "FreeBSD" -> return "freebsd"
        else -> {
            throw IllegalArgumentException("we don't know ${os} value")
        }
    }
}

private fun defaultTarget() = detectHost()

private val knownTargets = mapOf(
    "linux" to "linux",
    "macbook" to "osx",
    "iphone" to "osx-ios",
    "iphone_sim" to "osx-ios-sim",
    "raspberrypi" to "linux-raspberrypi")


private fun String.targetSuffix(): String =
    knownTargets[this] ?: error("Unsupported target $this.")

// Performs substitution similar to:
//  foo = ${foo} ${foo.${arch}} ${foo.${os}}
private fun substitute(properties: Properties, substitutions: Map<String, String>) {
    for (key in properties.stringPropertyNames()) {
        if (key.contains('.')) {
            continue
        }
        var value = ""
        for (substitution in substitutions.keys) {
            val property = properties.getProperty(key + "." + substitutions[substitution])
            if (property != null) {
                value += " " + property
            }
        }
        if (value != "") {
            properties.setProperty(key, properties.getProperty(key, "") + " " + value)
        }
    }
}

private fun getArgPrefix(arg: String): String {
    val index = arg.indexOf(':')
    if (index == -1) {
        return ""
    } else {
        return arg.substring(0, index)
    }
}

private fun dropPrefix(arg: String): String {
    val index = arg.indexOf(':')
    if (index == -1) {
        return ""
    } else {
        return arg.substring(index + 1)
    }
}

private fun ProcessBuilder.runExpectingSuccess() {
    val res = this.start().waitFor()
    if (res != 0) {
        throw Error("Process finished with non-zero exit code: $res")
    }
}

private fun Properties.getSpaceSeparated(name: String): List<String> {
    return this.getProperty(name)?.split(' ')?.filter { it.isNotEmpty() } ?: emptyList()
}

private fun <T> Collection<T>.atMostOne(): T? {
    return when (this.size) {
        0 -> null
        1 -> this.iterator().next()
        else -> throw IllegalArgumentException("Collection has more than one element.")
    }
}

private fun Properties.getOsSpecific(name: String, 
    host: String = detectHost()): String? {

    return this.getProperty("$name.$host")
}

private fun List<String>?.isTrue(): Boolean {
    // The rightmost wins, null != "true".
    return this?.last() == "true"
}

private fun runCmd(command: Array<String>, workDir: File, verbose: Boolean = false) {
    val builder = ProcessBuilder(*command)
            .directory(workDir)

    val logFile: File?

    if (verbose) {
        println(command.joinToString(" "))
        builder.inheritIO()
        logFile = null
    } else {
        logFile = createTempFile(suffix = ".log")
        logFile.deleteOnExit()

        builder.redirectOutput(ProcessBuilder.Redirect.to(logFile))
                .redirectErrorStream(true)
    }

    try {
        builder.runExpectingSuccess()
    } catch (e: Throwable) {
        if (!verbose) {
            println(command.joinToString(" "))
            logFile!!.useLines {
                it.forEach { println(it) }
            }
        }

        throw e
    }
}

private fun maybeExecuteHelper(dependenciesRoot: String, properties: Properties, dependencies: List<String>) {
    try {
        val kClass = Class.forName("org.jetbrains.kotlin.konan.Helper0").kotlin
        @Suppress("UNCHECKED_CAST")
        val ctor = kClass.constructors.single() as KFunction<Runnable>
        val result = ctor.call(dependenciesRoot, properties, dependencies)
        result.run()
    } catch (notFound: ClassNotFoundException) {
        // Just ignore, no helper.
    } catch (e: Throwable) {
        throw IllegalStateException("Cannot download dependencies.", e)
    }
}

private fun Properties.defaultCompilerOpts(target: String, dependencies: String): List<String> {

    val hostSysRootDir = this.getOsSpecific("sysRoot", target)!!
    val hostSysRoot = "$dependencies/$hostSysRootDir"
    val targetSysRootDir = this.getOsSpecific("targetSysRoot", target) ?: hostSysRootDir
    val targetSysRoot = "$dependencies/$targetSysRootDir"
    val sysRoot = targetSysRoot
    val llvmHomeDir = this.getOsSpecific("llvmHome", target)!!
    val llvmHome = "$dependencies/$llvmHomeDir"
    val llvmVersion = this.getProperty("llvmVersion")!!

    val dependencyList = getOsSpecific("dependencies", target)?.split(' ') ?: listOf<String>()
    maybeExecuteHelper(dependencies, this, dependencyList)

    // StubGenerator passes the arguments to libclang which 
    // works not exactly the same way as the clang binary and 
    // (in particular) uses different default header search path.
    // See e.g. http://lists.llvm.org/pipermail/cfe-dev/2013-November/033680.html
    // We workaround the problem with -isystem flag below.
    val isystem = "$llvmHome/lib/clang/$llvmVersion/include"

    when (target) {
        "osx" -> 
            return listOf(
                "-isystem", isystem,
                "-B$hostSysRoot/usr/bin",
                "--sysroot=$sysRoot",
                "-mmacosx-version-min=10.10")
        "osx-ios" -> 
            return listOf(
                "-arch", "arm64",
                "-isystem", isystem,
                "-B$hostSysRoot/usr/bin",
                "--sysroot=$sysRoot",
                "-miphoneos-version-min=5.0.0")
        "osx-ios-sim" -> 
            return listOf(
                "-arch", "x86_64",
                "-isystem", isystem,
                "-B$hostSysRoot/usr/bin",
                "--sysroot=$sysRoot",
                "-mios-simulator-version-min=5.0.0")
        "linux" -> {
            val gccToolChainDir = this.getOsSpecific("gccToolChain", target)!!
            val gccToolChain= "$dependencies/$gccToolChainDir"

            return listOf(
                "-isystem", isystem,
                "--gcc-toolchain=$gccToolChain",
                "-L$llvmHome/lib",
                "-B$hostSysRoot/../bin",
                "--sysroot=$sysRoot")
        }
        "linux-raspberrypi" -> {
            val gccToolChainDir = this.getOsSpecific("gccToolChain", target)!!
            val gccToolChain= "$dependencies/$gccToolChainDir"

            return listOf(
                "-target", "armv7-unknown-linux-gnueabihf",
                "-isystem", isystem,
                "--gcc-toolchain=$gccToolChain",
                "-L$llvmHome/lib",
                "-B$hostSysRoot/../bin",
                "--sysroot=$sysRoot")
        }
        else -> error("Unexpected target: ${target}")
    }
}

private fun loadProperties(file: File?, substitutions: Map<String, String>): Properties {
    val result = Properties()
    file?.bufferedReader()?.use { reader ->
        result.load(reader)
    }
    substitute(result, substitutions)
    return result
}

private fun usage() {
    println("""
Run interop tool with -def:<def_file_for_lib>.def
Following flags are supported:
  -def:<file>.def specifies library definition file
  -copt:<c compiler flags> specifies flags passed to clang
  -lopt:<linker flags> specifies flags passed to linker
  -verbose increases verbosity
  -shims adds generation of shims tracing native library calls
  -pkg:<fully qualified package name>
  -h:<file>.h header files to parse
""")
}

private fun processLib(konanHome: String,
                       substitutions: Map<String, String>,
                       commandArgs: List<String>) {

    val args = commandArgs.groupBy ({ getArgPrefix(it) }, { dropPrefix(it) }) // TODO

    val userDir = System.getProperty("user.dir")
    val ktGenRoot = args["-generated"]?.single() ?: userDir
    val nativeLibsDir = args["-natives"]?.single() ?: userDir
    val flavorName = args["-flavor"]?.single() ?: "jvm"
    val flavor = KotlinPlatform.values().single { it.name.equals(flavorName, ignoreCase = true) }
    val target = args["-target"]?.single()?.targetSuffix() ?: defaultTarget()
    val defFile = args["-def"]?.single()?.let { File(it) }

    if (defFile == null && args["-pkg"] == null) {
        usage()
        return
    }

    val config = loadProperties(defFile, substitutions)

    val konanFileName = args["-properties"]?.single() ?:
        "${konanHome}/konan/konan.properties"
    val konanFile = File(konanFileName)
    val konanProperties = loadProperties(konanFile, mapOf())

    // TODO: We can provide a set of flags to find the components in the absence of 'dist' or 'dist/dependencies'.
    val llvmHome = konanProperties.getOsSpecific("llvmHome")!!
    val dependencies =  "$konanHome/dependencies"
    val llvmInstallPath = "$dependencies/$llvmHome"
    val additionalHeaders = args["-h"].orEmpty()
    val additionalCompilerOpts = args["-copt"].orEmpty()
    val additionalLinkerOpts = args["-lopt"].orEmpty()
    val generateShims = args["-shims"].isTrue()
    val verbose = args["-verbose"].isTrue()

    val defaultOpts = konanProperties.defaultCompilerOpts(target, dependencies)
    val headerFiles = config.getSpaceSeparated("headers") + additionalHeaders
    val compilerOpts = 
        config.getSpaceSeparated("compilerOpts") +
        defaultOpts + additionalCompilerOpts 
    val compiler = "clang"
    val language = Language.C
    val excludeSystemLibs = config.getProperty("excludeSystemLibs")?.toBoolean() ?: false

    val entryPoint = config.getSpaceSeparated("entryPoint").singleOrNull()
    val linkerOpts = 
        config.getSpaceSeparated("linkerOpts").toTypedArray() +
        defaultOpts + additionalLinkerOpts 
    val linker = args["-linker"]?.singleOrNull() ?: config.getProperty("linker") ?: "clang"
    val excludedFunctions = config.getSpaceSeparated("excludedFunctions").toSet()

    val fqParts = args["-pkg"]?.singleOrNull()?.let {
        it.split('.')
    } ?: defFile!!.name.split('.').reversed().drop(1)

    val outKtFileName = fqParts.last() + ".kt"

    val outKtPkg = fqParts.joinToString(".")
    val outKtFileRelative = (fqParts + outKtFileName).joinToString("/")
    val outKtFile = File(ktGenRoot, outKtFileRelative)

    val libName = args["-cstubsname"]?.atMostOne() ?: fqParts.joinToString("") + "stubs"

    val library = NativeLibrary(headerFiles, compilerOpts, language, excludeSystemLibs)
    val configuration = InteropConfiguration(
            library = library,
            pkgName = outKtPkg,
            excludedFunctions = excludedFunctions,
            strictEnums = config.getSpaceSeparated("strictEnums").toSet(),
            nonStrictEnums = config.getSpaceSeparated("nonStrictEnums").toSet()
    )

    val nativeIndex = buildNativeIndex(library)

    val gen = StubGenerator(nativeIndex, configuration, libName, generateShims, verbose, flavor)

    outKtFile.parentFile.mkdirs()
    outKtFile.bufferedWriter().use { out ->
        gen.withOutput({ out.appendln(it) }) {
            gen.generateKotlinFile()
        }
    }

    File(nativeLibsDir).mkdirs()

    val outCFile = File("$nativeLibsDir/$libName.c") // TODO: select the better location.

    outCFile.bufferedWriter().use { out ->
        gen.withOutput({ out.appendln(it) }) {
            gen.generateCFile(headerFiles, entryPoint)
        }
    }

    val workDir = defFile?.parentFile ?: File(System.getProperty("java.io.tmpdir"))

    if (flavor == KotlinPlatform.JVM) {

        val outOFile = createTempFile(suffix = ".o")

        val javaHome = System.getProperty("java.home")
        val compilerArgsForJniIncludes = listOf("", "linux", "darwin").map { "-I$javaHome/../include/$it" }.toTypedArray()

        val compilerCmd = arrayOf("$llvmInstallPath/bin/$compiler", *compilerOpts.toTypedArray(),
                *compilerArgsForJniIncludes,
                "-c", outCFile.path, "-o", outOFile.path)

        runCmd(compilerCmd, workDir, verbose)

        val outLib = nativeLibsDir + "/" + System.mapLibraryName(libName)

        val linkerCmd = arrayOf("$llvmInstallPath/bin/$linker", *linkerOpts, outOFile.path, "-shared", "-o", outLib,
                "-Wl,-flat_namespace,-undefined,dynamic_lookup")

        runCmd(linkerCmd, workDir, verbose)

        outOFile.delete()
    } else if (flavor == KotlinPlatform.NATIVE) {
        val outBcName = libName + ".bc"
        val outLib = nativeLibsDir + "/" + outBcName
        val compilerCmd = arrayOf("$llvmInstallPath/bin/$compiler", *compilerOpts.toTypedArray(),
                "-emit-llvm", "-c", outCFile.path, "-o", outLib)

        runCmd(compilerCmd, workDir, verbose)
    }
}
