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

import org.jetbrains.kotlin.native.interop.indexer.*
import java.io.File
import java.io.StringReader
import java.lang.IllegalArgumentException
import java.util.*
import kotlin.reflect.KFunction

fun main(args: Array<String>) {
    val konanHome = File(System.getProperty("konan.home")).absolutePath

    // TODO: remove OSX defaults.
    val substitutions = mapOf(
            "arch" to (System.getenv("TARGET_ARCH") ?: "x86-64"),
            "os" to (System.getenv("TARGET_OS") ?: detectHost())
    )

    processLib(konanHome, substitutions, parseArgs(args))
}

private fun parseArgs(args: Array<String>): Map<String, List<String>> {
    val commandLine = mutableMapOf<String, MutableList<String>>()
    for(index in 0..args.size-1 step 2) {
        val key = args[index]
        if (key[0] != '-') {
            throw IllegalArgumentException("Expected a flag with initial dash: $key")
        }
        if (index+1 == args.size) {
            throw IllegalArgumentException("Expected an value after $key")
        }
        val value = args[index+1]
        commandLine[key] ?. add(value) ?: commandLine.put(key, mutableListOf(value))
    }
    return commandLine
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
    "host" to defaultTarget(),
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
        for (substitution in substitutions.values) {
            val suffix = ".$substitution"
            if (key.endsWith(suffix)) {
                val baseKey = key.removeSuffix(suffix)
                val oldValue = properties.getProperty(baseKey, "")
                val appendedValue = properties.getProperty(key, "")
                val newValue = if (oldValue != "") "$oldValue $appendedValue" else appendedValue
                properties.setProperty(baseKey, newValue)
            }
        }
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

private fun String.matchesToGlob(glob: String): Boolean =
        java.nio.file.FileSystems.getDefault()
                .getPathMatcher("glob:$glob").matches(java.nio.file.Paths.get(this))

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

    val arch = this.getOsSpecific("arch", target)!!
    val hostSysRootDir = this.getOsSpecific("sysRoot")!!
    val hostSysRoot = "$dependencies/$hostSysRootDir"
    val targetSysRootDir = this.getOsSpecific("targetSysRoot", target) ?: hostSysRootDir
    val targetSysRoot = "$dependencies/$targetSysRootDir"
    val sysRoot = targetSysRoot
    val llvmHomeDir = this.getOsSpecific("llvmHome")!!
    val llvmHome = "$dependencies/$llvmHomeDir"

    System.load("$llvmHome/lib/${System.mapLibraryName("clang")}")

    val llvmVersion = this.getProperty("llvmVersion")!!

    // StubGenerator passes the arguments to libclang which 
    // works not exactly the same way as the clang binary and 
    // (in particular) uses different default header search path.
    // See e.g. http://lists.llvm.org/pipermail/cfe-dev/2013-November/033680.html
    // We workaround the problem with -isystem flag below.
    val isystem = "$llvmHome/lib/clang/$llvmVersion/include"

    when (detectHost()) {
        "osx" -> {
            val osVersionMinFlag = this.getOsSpecific("osVersionMinFlagClang", target)!!
            val osVersionMinValue = this.getOsSpecific("osVersionMin", target)!!

            return listOf(
                "-arch", arch,
                "-isystem", isystem,
                "-B$hostSysRoot/usr/bin",
                "--sysroot=$sysRoot",
                "$osVersionMinFlag=$osVersionMinValue")
        }
        "linux" -> {
            val gccToolChainDir = this.getOsSpecific("gccToolChain", target)!!
            val gccToolChain= "$dependencies/$gccToolChainDir"
            val quadruple = this.getOsSpecific("quadruple", target)!!

            return listOf(
                "-target", quadruple,
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

private fun parseDefFile(file: File?, substitutions: Map<String, String>): Pair<Properties, List<String>> {
    val properties = Properties()

    if (file == null) {
        return properties to emptyList()
    }

    val lines = file.readLines()

    val separator = "---"
    val separatorIndex = lines.indexOf(separator)

    val propertyLines: List<String>
    val headerLines: List<String>

    if (separatorIndex != -1) {
        propertyLines = lines.subList(0, separatorIndex)
        headerLines = lines.subList(separatorIndex + 1, lines.size)
    } else {
        propertyLines = lines
        headerLines = emptyList()
    }

    val propertiesReader = StringReader(propertyLines.joinToString(System.lineSeparator()))
    properties.load(propertiesReader)
    substitute(properties, substitutions)

    return properties to headerLines
}

private fun usage() {
    println("""
Run interop tool with -def <def_file_for_lib>.def
Following flags are supported:
  -def <file>.def specifies library definition file
  -copt <c compiler flags> specifies flags passed to clang
  -lopt <linker flags> specifies flags passed to linker
  -verbose <boolean> increases verbosity
  -shims <boolean> adds generation of shims tracing native library calls
  -pkg <fully qualified package name> place the resulting definitions into the package
  -h <file>.h header files to parse
""")
}

private fun downloadDependencies(dependenciesRoot: String, target: String, konanProperties: Properties) {
    val dependencyList = konanProperties.getOsSpecific("dependencies", target)?.split(' ') ?: listOf<String>()
    maybeExecuteHelper(dependenciesRoot, konanProperties, dependencyList)
}

private fun processLib(konanHome: String,
                       substitutions: Map<String, String>,
                       args: Map<String, List<String>>) {

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

    val (config, defHeaderLines) = parseDefFile(defFile, substitutions)

    val konanFileName = args["-properties"]?.single() ?:
        "${konanHome}/konan/konan.properties"
    val konanFile = File(konanFileName)
    val konanProperties = loadProperties(konanFile, mapOf())
    val dependencies =  "$konanHome/dependencies"
    downloadDependencies(dependencies, target, konanProperties)

    // TODO: We can provide a set of flags to find the components in the absence of 'dist' or 'dist/dependencies'.
    val llvmHome = konanProperties.getOsSpecific("llvmHome")!!
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
    val excludeDependentModules = config.getProperty("excludeDependentModules")?.toBoolean() ?: false

    val entryPoint = config.getSpaceSeparated("entryPoint").atMostOne()
    val linkerOpts = 
        config.getSpaceSeparated("linkerOpts").toTypedArray() +
        defaultOpts + additionalLinkerOpts 
    val linker = args["-linker"]?.atMostOne() ?: config.getProperty("linker") ?: "clang"
    val excludedFunctions = config.getSpaceSeparated("excludedFunctions").toSet()

    val fqParts = args["-pkg"]?.atMostOne()?.let {
        it.split('.')
    } ?: defFile!!.name.split('.').reversed().drop(1)

    val outKtFileName = fqParts.last() + ".kt"

    val outKtPkg = fqParts.joinToString(".")
    val outKtFileRelative = (fqParts + outKtFileName).joinToString("/")
    val outKtFile = File(ktGenRoot, outKtFileRelative)

    val libName = args["-cstubsname"]?.atMostOne() ?: fqParts.joinToString("") + "stubs"

    val headerFilterGlobs = config.getSpaceSeparated("headerFilter")

    val headerFilter = { name: String ->
        if (headerFilterGlobs.isEmpty()) {
            true
        } else {
            headerFilterGlobs.any { name.matchesToGlob(it) }
        }
    }

    val library = NativeLibrary(
            includes = headerFiles,
            additionalPreambleLines = defHeaderLines,
            compilerArgs = compilerOpts,
            language = language,
            excludeSystemLibs = excludeSystemLibs,
            excludeDepdendentModules = excludeDependentModules,
            headerFilter = headerFilter
    )

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

    File(nativeLibsDir).mkdirs()
    val outCFile = File("$nativeLibsDir/$libName.c") // TODO: select the better location.

    outKtFile.bufferedWriter().use { ktFile ->
        outCFile.bufferedWriter().use { cFile ->
            gen.generateFiles(ktFile = ktFile, cFile = cFile, entryPoint = entryPoint)
        }
    }

    val workDir = defFile?.absoluteFile?.parentFile ?: File(userDir)

    if (flavor == KotlinPlatform.JVM) {

        val outOFile = createTempFile(suffix = ".o")

        val compilerCmd = arrayOf("$llvmInstallPath/bin/$compiler", *gen.libraryForCStubs.compilerArgs.toTypedArray(),
                "-c", outCFile.absolutePath, "-o", outOFile.absolutePath)

        runCmd(compilerCmd, workDir, verbose)

        val outLib = File(nativeLibsDir, System.mapLibraryName(libName))

        val linkerCmd = arrayOf("$llvmInstallPath/bin/$linker", *linkerOpts, outOFile.absolutePath, "-shared",
                "-o", outLib.absolutePath,
                "-Wl,-flat_namespace,-undefined,dynamic_lookup")

        runCmd(linkerCmd, workDir, verbose)

        outOFile.delete()
    } else if (flavor == KotlinPlatform.NATIVE) {
        val outBcName = libName + ".bc"
        val outLib = File(nativeLibsDir, outBcName)
        val compilerCmd = arrayOf("$llvmInstallPath/bin/$compiler", *gen.libraryForCStubs.compilerArgs.toTypedArray(),
                "-emit-llvm", "-c", outCFile.absolutePath, "-o", outLib.absolutePath)

        runCmd(compilerCmd, workDir, verbose)
    }

    if (!args["-keepcstubs"].isTrue()) {
        outCFile.delete()
    }
}
