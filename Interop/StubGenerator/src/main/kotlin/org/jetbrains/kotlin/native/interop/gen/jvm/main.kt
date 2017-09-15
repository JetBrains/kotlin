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

import org.jetbrains.kotlin.native.interop.gen.HeadersInclusionPolicyImpl
import org.jetbrains.kotlin.native.interop.gen.ImportsImpl
import org.jetbrains.kotlin.native.interop.indexer.*
import java.io.File
import java.io.StringReader
import java.lang.IllegalArgumentException
import java.util.*
import kotlin.reflect.KFunction

fun main(args: Array<String>) = interop(args, null)

fun interop(args: Array<String>, argsToCompiler: MutableList<String>? = null) {
    val konanHome = File(System.getProperty("konan.home")).absolutePath

    val substitutions = mapOf(
            "arch" to (System.getenv("TARGET_ARCH") ?: "x86-64"),
            "os" to (System.getenv("TARGET_OS") ?: host)
    )
    processLib(konanHome, substitutions, parseArgs(args), argsToCompiler)
}

// Options, whose values are space-separated and can be escaped.
val escapedOptions = setOf("-compilerOpts", "-linkerOpts")

private fun String.asArgList(key: String) =
        if (escapedOptions.contains(key))
            this.split(Regex("(?<!\\\\)\\Q \\E")).filter { it.isNotEmpty() }.map { it.replace("\\ ", " ") }
        else
            listOf(this)

private fun parseArgs(args: Array<String>): Map<String, List<String>> {
    val commandLine = mutableMapOf<String, MutableList<String>>()
    for (index in 0..args.size - 1 step 2) {
        val key = args[index]
        if (key[0] != '-') {
            throw IllegalArgumentException("Expected a flag with initial dash: $key")
        }
        if (index + 1 == args.size) {
            throw IllegalArgumentException("Expected an value after $key")
        }
        val value = args[index + 1].asArgList(key)
        commandLine[key]?.addAll(value) ?: commandLine.put(key, value.toMutableList())
    }
    return commandLine
}

private val host: String by lazy {
    val os = System.getProperty("os.name")
    when {
        os == "Linux" -> "linux"
        os.startsWith("Windows") -> "mingw"
        os == "Mac OS X" -> "osx"
        os == "FreeBSD" -> "freebsd"
        else -> {
            throw IllegalArgumentException("we don't know ${os} value")
        }
    }
}

private val defaultTarget: String by lazy {
    host
}

// TODO: share KonanTarget class here.
private val knownTargets = mapOf(
        "host" to host,
        "linux" to "linux",
        "macbook" to "osx",
        "osx" to "osx",
        "iphone" to "ios",
        "ios" to "ios",
        "iphone_sim" to "ios_sim",
        "ios_sim" to "ios_sim",
        "raspberrypi" to "raspberrypi",
        "mips" to "mips",
        "android_arm32" to "android_arm32",
        "android_arm64" to "android_arm64",
        "mingw" to "mingw",
        "wasm32" to "wasm32"
)


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

private fun Properties.getHostSpecific(
        name: String) = getProperty("$name.$host")

private fun Properties.getTargetSpecific(
        name: String, target: String) = getProperty("$name.$target")

private fun Properties.getHostTargetSpecific(
        name: String, target: String) = if (host != target)
    getProperty("$name.$host-$target")
else
    getProperty("$name.$host")

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
        val kClass = Class.forName("org.jetbrains.kotlin.konan.util.Helper0").kotlin
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

private fun Properties.getClangFlags(target: String, targetSysRoot: String): List<String> {
    val flags = getTargetSpecific("clangFlags", target)
    if (flags == null) return emptyList()
    return flags.replace("<sysrootDir>", targetSysRoot).split(' ')
}

private fun Properties.defaultCompilerOpts(target: String, dependencies: String): List<String> {
    val targetToolchainDir = getHostTargetSpecific("targetToolchain", target)!!
    val targetToolchain = "$dependencies/$targetToolchainDir"
    val targetSysRootDir = getTargetSpecific("targetSysRoot", target)!!
    val targetSysRoot = "$dependencies/$targetSysRootDir"
    val llvmHomeDir = getHostSpecific("llvmHome")!!
    val llvmHome = "$dependencies/$llvmHomeDir"

    val libclang = when (host) {
        "mingw" -> "$llvmHome/bin/libclang.dll"
        else -> "$llvmHome/lib/${System.mapLibraryName("clang")}"
    }
    System.load(libclang)

    val llvmVersion = getHostSpecific("llvmVersion")!!

    // StubGenerator passes the arguments to libclang which
    // works not exactly the same way as the clang binary and
    // (in particular) uses different default header search path.
    // See e.g. http://lists.llvm.org/pipermail/cfe-dev/2013-November/033680.html
    // We workaround the problem with -isystem flag below.
    val isystem = "$llvmHome/lib/clang/$llvmVersion/include"
    val quadruple = getTargetSpecific("quadruple", target)
    val arch = getTargetSpecific("arch", target)
    val archSelector = if (quadruple != null)
        listOf("-target", quadruple) else listOf("-arch", arch!!)
    val commonArgs = listOf("-isystem", isystem, "--sysroot=$targetSysRoot") + getClangFlags(target, targetSysRoot)
    when (host) {
        "osx" -> {
            val osVersionMinFlag = getTargetSpecific("osVersionMinFlagClang", target)
            val osVersionMinValue = getTargetSpecific("osVersionMin", target)
            return archSelector + commonArgs + listOf("-B$targetToolchain/bin") +
                    (if (osVersionMinFlag != null && osVersionMinValue != null)
                        listOf("$osVersionMinFlag=$osVersionMinValue") else emptyList())
        }
        "linux" -> {
            val libGcc = getTargetSpecific("libGcc", target)
            val binDir = "$targetSysRoot/${libGcc ?: "bin"}"
            return archSelector + commonArgs + listOf(
                    "-B$binDir", "--gcc-toolchain=$targetToolchain",
                    "-fuse-ld=$targetToolchain/bin/ld")
        }
        "mingw" -> {
            return archSelector + commonArgs + listOf("-B$targetSysRoot/bin")
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

private fun Properties.storeProperties(file: File) {
    file.outputStream().use {
        this.store(it, null)
    }
}

private fun parseDefFile(file: File?, substitutions: Map<String, String>): Triple<Properties, Properties, List<String>> {
    val properties = Properties()

    if (file == null) {
        return Triple(properties, Properties(), emptyList())
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

    // Pass unsubstituted copy of properties we have obtained from `.def` 
    // to compiler `-maniest`.
    val manifestAddendProperties = properties.duplicate()

    substitute(properties, substitutions)

    return Triple(properties, manifestAddendProperties, headerLines)
}

private fun Properties.duplicate() = Properties().apply { putAll(this@duplicate) }

private fun usage() {
    println("""
Run interop tool with -def <def_file_for_lib>.def
Following flags are supported:
  -def <file>.def specifies library definition file
  -compilerOpts <c compiler flags> specifies flags passed to clang
  -linkerOpts <linker flags> specifies flags passed to linker
  -verbose <boolean> increases verbosity
  -shims <boolean> adds generation of shims tracing native library calls
  -pkg <fully qualified package name> place the resulting definitions into the package
  -h <file>.h header files to parse
""")
}

private fun downloadDependencies(dependenciesRoot: String, target: String, konanProperties: Properties) {
    val dependencyList = konanProperties.getHostTargetSpecific("dependencies", target)?.split(' ') ?: listOf<String>()
    maybeExecuteHelper(dependenciesRoot, konanProperties, dependencyList)
}

private fun selectNativeLanguage(config: Properties): Language {
    val languages = mapOf(
            "C" to Language.C,
            "Objective-C" to Language.OBJECTIVE_C
    )

    val language = config.getProperty("language") ?: return Language.C

    return languages[language] ?:
            error("Unexpected language '$language'. Possible values are: ${languages.keys.joinToString { "'$it'" }}")
}

private fun resolveLibraries(staticLibraries: List<String>, libraryPaths: List<String>): List<String> {
    val result = mutableListOf<String>()
    staticLibraries.forEach { library ->
        
        val resolution = libraryPaths.map { "$it/$library" } 
                .find { File(it).exists() }

        if (resolution != null) {
            result.add(resolution)
        } else {
            error("Could not find '$library' binary in neither of $libraryPaths")
        }
    }
    return result
}

private fun argsToCompiler(staticLibraries: List<String>, libraryPaths: List<String>): List<String> {
    return resolveLibraries(staticLibraries, libraryPaths)
        .map { it -> listOf("-includeBinary", it) } .flatten()
}

private fun parseImports(args: Map<String, List<String>>): ImportsImpl {
    val headerIdToPackage = (args["-import"] ?: emptyList()).map { arg ->
        val (pkg, joinedIds) = arg.split(':')
        val ids = joinedIds.split(',')
        ids.map { HeaderId(it) to pkg }
    }.reversed().flatten().toMap()

    return ImportsImpl(headerIdToPackage)
}

private fun processLib(konanHome: String,
                       substitutions: Map<String, String>,
                       args: Map<String, List<String>>, 
                       argsToCompiler: MutableList<String>?) {

    val userDir = System.getProperty("user.dir")
    val ktGenRoot = args["-generated"]?.single() ?: userDir
    val nativeLibsDir = args["-natives"]?.single() ?: userDir
    val flavorName = args["-flavor"]?.single() ?: "jvm"
    val flavor = KotlinPlatform.values().single { it.name.equals(flavorName, ignoreCase = true) }
    val target = args["-target"]?.single()?.targetSuffix() ?: defaultTarget
    val defFile = args["-def"]?.single()?.let { File(it) }
    val manifestAddend = args["-manifest"]?.single()?.let { File(it) }

    if (defFile == null && args["-pkg"] == null) {
        usage()
        return
    }

    val (config, manifestAddendProperties, defHeaderLines)
            = parseDefFile(defFile, substitutions)

    val konanFileName = args["-properties"]?.single() ?:
            "${konanHome}/konan/konan.properties"
    val konanFile = File(konanFileName)
    val konanProperties = loadProperties(konanFile, mapOf())
    val dependencies = "$konanHome/dependencies"
    downloadDependencies(dependencies, target, konanProperties)

    // TODO: We can provide a set of flags to find the components in the absence of 'dist' or 'dist/dependencies'.
    val llvmHome = konanProperties.getHostSpecific("llvmHome")!!
    val llvmInstallPath = "$dependencies/$llvmHome"
    val additionalHeaders = args["-h"].orEmpty()
    val additionalCompilerOpts = args["-copt"].orEmpty() + args["-compilerOpts"].orEmpty()
    val additionalLinkerOpts = args["-lopt"].orEmpty() + args["-linkerOpts"].orEmpty()
    val generateShims = args["-shims"].isTrue()
    val verbose = args["-verbose"].isTrue()

    val defaultOpts = konanProperties.defaultCompilerOpts(target, dependencies)
    val headerFiles = config.getSpaceSeparated("headers") + additionalHeaders
    val language = selectNativeLanguage(config)
    val compilerOpts: List<String> = mutableListOf<String>().apply {
        addAll(config.getSpaceSeparated("compilerOpts"))
        addAll(defaultOpts)
        addAll(additionalCompilerOpts)
        addAll(when (language) {
            Language.C -> emptyList()
            Language.OBJECTIVE_C -> {
                // "Objective-C" within interop means "Objective-C with ARC":
                listOf("-fobjc-arc")
                // Using this flag here has two effects:
                // 1. The headers are parsed with ARC enabled, thus the API is visible correctly.
                // 2. The generated Objective-C stubs are compiled with ARC enabled, so reference counting
                // calls are inserted automatically.
            }
        })
    }
    val compiler = "clang"
    val excludeSystemLibs = config.getProperty("excludeSystemLibs")?.toBoolean() ?: false
    val excludeDependentModules = config.getProperty("excludeDependentModules")?.toBoolean() ?: false

    val entryPoint = config.getSpaceSeparated("entryPoint").atMostOne()
    val linkerOpts =
            config.getSpaceSeparated("linkerOpts").toTypedArray() + defaultOpts + additionalLinkerOpts
    val linker = args["-linker"]?.atMostOne() ?: config.getProperty("linker") ?: "clang"
    val excludedFunctions = config.getSpaceSeparated("excludedFunctions").toSet()
    val staticLibraries = config.getSpaceSeparated("staticLibraries") + args["-staticLibrary"].orEmpty()
    val libraryPaths = config.getSpaceSeparated("libraryPaths") + args["-libraryPath"].orEmpty()
    argsToCompiler ?. let { it.addAll(argsToCompiler(staticLibraries, libraryPaths)) }

    val fqParts = (args["-pkg"]?.atMostOne() ?: config.getProperty("package"))?.let {
        it.split('.')
    } ?: defFile!!.name.split('.').reversed().drop(1)

    val outKtFileName = fqParts.last() + ".kt"

    val outKtPkg = fqParts.joinToString(".")
    val outKtFileRelative = (fqParts + outKtFileName).joinToString("/")
    val outKtFile = File(ktGenRoot, outKtFileRelative)

    val libName = args["-cstubsname"]?.atMostOne() ?: fqParts.joinToString("") + "stubs"

    val headerFilterGlobs = config.getSpaceSeparated("headerFilter")
    val imports = parseImports(args)
    val headerInclusionPolicy = HeadersInclusionPolicyImpl(headerFilterGlobs, imports)

    val library = NativeLibrary(
            includes = headerFiles,
            additionalPreambleLines = defHeaderLines,
            compilerArgs = compilerOpts,
            language = language,
            excludeSystemLibs = excludeSystemLibs,
            excludeDepdendentModules = excludeDependentModules,
            headerInclusionPolicy = headerInclusionPolicy
    )

    val configuration = InteropConfiguration(
            library = library,
            pkgName = outKtPkg,
            excludedFunctions = excludedFunctions,
            strictEnums = config.getSpaceSeparated("strictEnums").toSet(),
            nonStrictEnums = config.getSpaceSeparated("nonStrictEnums").toSet()
    )

    val nativeIndex = buildNativeIndex(library)

    val gen = StubGenerator(nativeIndex, configuration, libName, generateShims, verbose, flavor, imports)

    outKtFile.parentFile.mkdirs()

    File(nativeLibsDir).mkdirs()
    val outCFile = File("$nativeLibsDir/$libName.${language.sourceFileExtension}") // TODO: select the better location.

    outKtFile.bufferedWriter().use { ktFile ->
        outCFile.bufferedWriter().use { cFile ->
            gen.generateFiles(ktFile = ktFile, cFile = cFile, entryPoint = entryPoint)
        }
    }

    // TODO: if a library has partially included headers, then it shouldn't be used as a dependency.
    manifestAddendProperties["includedHeaders"] = nativeIndex.includedHeaders.joinToString(" ") { it.value }
    manifestAddendProperties["pkg"] = outKtPkg

    gen.addManifestProperties(manifestAddendProperties)

    manifestAddend?.parentFile?.mkdirs()
    manifestAddend?.let { manifestAddendProperties.storeProperties(it) }

    val workDir = defFile?.absoluteFile?.parentFile ?: File(userDir)

    if (flavor == KotlinPlatform.JVM) {

        val outOFile = createTempFile(suffix = ".o")

        val compilerCmd = arrayOf("$llvmInstallPath/bin/$compiler", *gen.libraryForCStubs.compilerArgs.toTypedArray(),
                "-c", outCFile.absolutePath, "-o", outOFile.absolutePath)

        runCmd(compilerCmd, workDir, verbose)

        val outLib = File(nativeLibsDir, System.mapLibraryName(libName))

        val linkerCmd = arrayOf("$llvmInstallPath/bin/$linker",
                outOFile.absolutePath, "-shared", "-o", outLib.absolutePath,
                *linkerOpts)

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
