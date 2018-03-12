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

import org.jetbrains.kotlin.konan.TempFiles
import org.jetbrains.kotlin.konan.exec.Command
import org.jetbrains.kotlin.konan.util.DefFile
import org.jetbrains.kotlin.native.interop.gen.HeadersInclusionPolicyImpl
import org.jetbrains.kotlin.native.interop.gen.ImportsImpl
import org.jetbrains.kotlin.native.interop.gen.argsToCompiler
import org.jetbrains.kotlin.native.interop.gen.wasm.processIdlLib
import org.jetbrains.kotlin.native.interop.indexer.*
import org.jetbrains.kotlin.native.interop.tool.*
import java.io.File
import java.lang.IllegalArgumentException
import java.nio.file.*
import java.util.*

fun main(args: Array<String>) {
    processCLib(args)
}

fun interop(flavor: String, args: Array<String>) = when(flavor) {
        "jvm", "native" -> processCLib(args)
        "wasm" -> processIdlLib(args)
        else -> error("Unexpected flavor")
    }

// Options, whose values are space-separated and can be escaped.
val escapedOptions = setOf("-compilerOpts", "-linkerOpts")

private fun String.asArgList(key: String) =
        if (escapedOptions.contains(key))
            this.split(Regex("(?<!\\\\)\\Q \\E")).filter { it.isNotEmpty() }.map { it.replace("\\ ", " ") }
        else
            listOf(this)

private fun <T> Collection<T>.atMostOne(): T? {
    return when (this.size) {
        0 -> null
        1 -> this.iterator().next()
        else -> throw IllegalArgumentException("Collection has more than one element.")
    }
}

private fun List<String>?.isTrue(): Boolean {
    // The rightmost wins, null != "true".
    return this?.last() == "true"
}

private fun runCmd(command: Array<String>, verbose: Boolean = false) {
    Command(*command).getOutputLines(true).let { lines ->
        if (verbose) lines.forEach(::println)
    }
}

private fun Properties.storeProperties(file: File) {
    file.outputStream().use {
        this.store(it, null)
    }
}

private fun Properties.putAndRunOnReplace(key: Any, newValue: Any, beforeReplace: (Any, Any, Any) -> Unit) {
    val oldValue = this[key]
    if (oldValue != null && oldValue != newValue) {
        beforeReplace(key, oldValue, newValue)
    }
    this[key] = newValue
}

// TODO: Utilize Usage from the big Kotlin.
// That requires to extend the CLITool class.
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

private fun selectNativeLanguage(config: DefFile.DefFileConfig): Language {
    val languages = mapOf(
            "C" to Language.C,
            "Objective-C" to Language.OBJECTIVE_C
    )

    val language = config.language ?: return Language.C

    return languages[language] ?:
            error("Unexpected language '$language'. Possible values are: ${languages.keys.joinToString { "'$it'" }}")
}

private fun parseImports(imports: Array<String>): ImportsImpl {
    val headerIdToPackage = imports.map { arg ->
        val (pkg, joinedIds) = arg.split(':')
        val ids = joinedIds.split(';')
        ids.map { HeaderId(it) to pkg }
    }.reversed().flatten().toMap()

    return ImportsImpl(headerIdToPackage)
}

fun getCompilerFlagsForVfsOverlay(headerFilterPrefix: Array<String>, def: DefFile): List<String> {
    val relativeToRoot = mutableMapOf<Path, Path>() // TODO: handle clashes

    val filteredIncludeDirs = headerFilterPrefix .map { Paths.get(it) }
    if (filteredIncludeDirs.isNotEmpty()) {
        val headerFilterGlobs = def.config.headerFilter
        if (headerFilterGlobs.isEmpty()) {
            error("'$HEADER_FILTER_ADDITIONAL_SEARCH_PREFIX' option requires " +
                    "'headerFilter' to be specified in .def file")
        }

        relativeToRoot += findFilesByGlobs(roots = filteredIncludeDirs, globs = headerFilterGlobs)
    }

    if (relativeToRoot.isEmpty()) {
        return emptyList()
    }

    val virtualRoot = Paths.get(System.getProperty("java.io.tmpdir")).resolve("konanSystemInclude")

    val virtualPathToReal = relativeToRoot.map { (relativePath, realRoot) ->
        virtualRoot.resolve(relativePath) to realRoot.resolve(relativePath)
    }.toMap()

    val vfsOverlayFile = createVfsOverlayFile(virtualPathToReal)

    return listOf("-I${virtualRoot.toAbsolutePath()}", "-ivfsoverlay", vfsOverlayFile.toAbsolutePath().toString())
}

private fun findFilesByGlobs(roots: List<Path>, globs: List<String>): Map<Path, Path> {
    val relativeToRoot = mutableMapOf<Path, Path>()

    val pathMatchers = globs.map { FileSystems.getDefault().getPathMatcher("glob:$it") }

    roots.reversed()
            .filter { path ->
                return@filter when {
                    path.toFile().exists() -> true
                    else -> { warn("$path doesn't exist"); false }
                }
            }
            .forEach { root ->
                // TODO: don't scan the entire tree, skip subdirectories according to globs.
                Files.walk(root, FileVisitOption.FOLLOW_LINKS).forEach { path ->
                    val relativePath = root.relativize(path)
                    if (!Files.isDirectory(path) && pathMatchers.any { it.matches(relativePath) }) {
                        relativeToRoot[relativePath] = root
                    }
                }
            }
    return relativeToRoot
}


private fun processCLib(args: Array<String>): Array<String>? {

    val arguments = parseCommandLine(args, CInteropArguments())
    val userDir = System.getProperty("user.dir")
    val ktGenRoot = arguments.generated ?: userDir
    val nativeLibsDir = arguments.natives ?: userDir
    val flavorName = arguments.flavor ?: "jvm"
    val flavor = KotlinPlatform.values().single { it.name.equals(flavorName, ignoreCase = true) }
    val defFile = arguments.def?.let { File(it) }
    val manifestAddend = arguments.manifest?.let { File(it) }

    if (defFile == null && arguments.pkg == null) {
        usage()
        return null
    }

    val tool = ToolConfig(
        arguments.target,
        flavor
    )
    tool.downloadDependencies()

    val def = DefFile(defFile, tool.substitutions)

    val additionalHeaders = arguments.header
    val additionalCompilerOpts = arguments.compilerOpts
    val additionalLinkerOpts = arguments.linkerOpts
    val generateShims = arguments.shims
    val verbose = arguments.verbose

    System.load(tool.libclang)

    val headerFiles = def.config.headers + additionalHeaders
    val language = selectNativeLanguage(def.config)
    val compilerOpts: List<String> = mutableListOf<String>().apply {
        addAll(def.config.compilerOpts)
        addAll(tool.defaultCompilerOpts)
        addAll(additionalCompilerOpts)
        addAll(getCompilerFlagsForVfsOverlay(arguments.headerFilterPrefix, def))
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

    val excludeSystemLibs = def.config.excludeSystemLibs
    val excludeDependentModules = def.config.excludeDependentModules

    val entryPoint = def.config.entryPoints.atMostOne()
    val linkerOpts =
            def.config.linkerOpts.toTypedArray() + 
            tool.defaultCompilerOpts + 
            additionalLinkerOpts
    val linkerName = arguments.linker ?: def.config.linker
    val linker = "${tool.llvmHome}/bin/$linkerName"
    val compiler = "${tool.llvmHome}/bin/clang"
    val excludedFunctions = def.config.excludedFunctions.toSet()
    val staticLibraries = def.config.staticLibraries + arguments.staticLibrary
    val libraryPaths = def.config.libraryPaths + arguments.libraryPath
    val fqParts = (arguments.pkg ?: def.config.packageName)?.let {
        it.split('.')
    } ?: defFile!!.name.split('.').reversed().drop(1)

    val outKtFileName = fqParts.last() + ".kt"

    val outKtPkg = fqParts.joinToString(".")
    val outKtFileRelative = (fqParts + outKtFileName).joinToString("/")
    val outKtFile = File(ktGenRoot, outKtFileRelative)

    val libName = arguments.cstubsname ?: fqParts.joinToString("") + "stubs"

    val tempFiles = TempFiles(libName, arguments.temporaryFilesDir)

    val headerFilterGlobs = def.config.headerFilter
    val imports = parseImports(arguments.import)
    val headerInclusionPolicy = HeadersInclusionPolicyImpl(headerFilterGlobs, imports)

    val library = NativeLibrary(
            includes = headerFiles,
            additionalPreambleLines = def.defHeaderLines,
            compilerArgs = compilerOpts + tool.platformCompilerOpts,
            headerToIdMapper = HeaderToIdMapper(sysRoot = tool.sysRoot),
            language = language,
            excludeSystemLibs = excludeSystemLibs,
            excludeDepdendentModules = excludeDependentModules,
            headerInclusionPolicy = headerInclusionPolicy
    )

    val configuration = InteropConfiguration(
            library = library,
            pkgName = outKtPkg,
            excludedFunctions = excludedFunctions,
            strictEnums = def.config.strictEnums.toSet(),
            nonStrictEnums = def.config.nonStrictEnums.toSet(),
            noStringConversion = def.config.noStringConversion.toSet()
    )

    val nativeIndex = buildNativeIndex(library)

    val gen = StubGenerator(nativeIndex, configuration, libName, generateShims, verbose, flavor, imports)

    outKtFile.parentFile.mkdirs()

    File(nativeLibsDir).mkdirs()
    val outCFile = tempFiles.create(libName, ".${language.sourceFileExtension}")

    outKtFile.bufferedWriter().use { ktFile ->
        File(outCFile.absolutePath).bufferedWriter().use { cFile ->
            gen.generateFiles(ktFile = ktFile, cFile = cFile, entryPoint = entryPoint)
        }
    }

    // TODO: if a library has partially included headers, then it shouldn't be used as a dependency.
    def.manifestAddendProperties["includedHeaders"] = nativeIndex.includedHeaders.joinToString(" ") { it.value }

    def.manifestAddendProperties.putAndRunOnReplace("package", outKtPkg) {
        _, oldValue, newValue ->
            warn("The package value `$oldValue` specified in .def file is overridden with explicit $newValue")
    }

    def.manifestAddendProperties["interop"] = "true"

    gen.addManifestProperties(def.manifestAddendProperties)

    manifestAddend?.parentFile?.mkdirs()
    manifestAddend?.let { def.manifestAddendProperties.storeProperties(it) }

    if (flavor == KotlinPlatform.JVM) {

        val outOFile = tempFiles.create(libName,".o")

        val compilerCmd = arrayOf(compiler, *gen.libraryForCStubs.compilerArgs.toTypedArray(),
                "-c", outCFile.absolutePath, "-o", outOFile.absolutePath)

        runCmd(compilerCmd, verbose)

        val outLib = File(nativeLibsDir, System.mapLibraryName(libName))

        val linkerCmd = arrayOf(linker,
                outOFile.absolutePath, "-shared", "-o", outLib.absolutePath,
                *linkerOpts)

        runCmd(linkerCmd, verbose)
    } else if (flavor == KotlinPlatform.NATIVE) {
        val outBcName = libName + ".bc"
        val outLib = File(nativeLibsDir, outBcName)
        val compilerCmd = arrayOf(compiler, *gen.libraryForCStubs.compilerArgs.toTypedArray(),
                "-emit-llvm", "-c", outCFile.absolutePath, "-o", outLib.absolutePath)

        runCmd(compilerCmd, verbose)
    }
    return argsToCompiler(staticLibraries, libraryPaths)
}
