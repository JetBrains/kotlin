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
import org.jetbrains.kotlin.native.interop.gen.*
import org.jetbrains.kotlin.native.interop.gen.wasm.processIdlLib
import org.jetbrains.kotlin.native.interop.indexer.*
import org.jetbrains.kotlin.native.interop.tool.*
import kotlinx.cli.ArgParser
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import java.lang.IllegalArgumentException
import java.nio.file.*
import java.util.*

fun main(args: Array<String>) {
    processCLib(args)
}

fun interop(flavor: String, args: Array<String>, additionalArgs: Map<String, Any> = mapOf()) =
        when(flavor) {
            "jvm", "native" -> processCLib(args, additionalArgs)
            "wasm" -> processIdlLib(args, additionalArgs)
            else -> error("Unexpected flavor")
        }

// Options, whose values are space-separated and can be escaped.
val escapedOptions = setOf("-compilerOpts", "-linkerOpts", "-compiler-options", "-linker-options")

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


private fun processCLib(args: Array<String>, additionalArgs: Map<String, Any> = mapOf()): Array<String> {
    val cinteropArguments = CInteropArguments()
    cinteropArguments.argParser.parse(args)
    val ktGenRoot = cinteropArguments.generated
    val nativeLibsDir = cinteropArguments.natives
    val flavorName = cinteropArguments.flavor
    val flavor = KotlinPlatform.values().single { it.name.equals(flavorName, ignoreCase = true) }
    val defFile = cinteropArguments.def?.let { File(it) }
    val manifestAddend = (additionalArgs["manifest"] as? String)?.let { File(it) }

    if (defFile == null && cinteropArguments.pkg == null) {
        cinteropArguments.argParser.printError("-def or -pkg should provided!")
    }

    val tool = prepareTool(cinteropArguments.target, flavor)

    val def = DefFile(defFile, tool.substitutions)
    val isLinkerOptsSetByUser = (cinteropArguments.linkerOpts.valueOrigin == ArgParser.ValueOrigin.SET_BY_USER) ||
            (cinteropArguments.linkerOptions.valueOrigin == ArgParser.ValueOrigin.SET_BY_USER) ||
            (cinteropArguments.linkerOption.valueOrigin == ArgParser.ValueOrigin.SET_BY_USER)
    if (flavorName == "native" && isLinkerOptsSetByUser) {
        warn("-linker-option(s)/-linkerOpts option is not supported by cinterop. Please add linker options to .def file or binary compilation instead.")
    }

    val additionalLinkerOpts = cinteropArguments.linkerOpts.value.toTypedArray() + cinteropArguments.linkerOption.value.toTypedArray() +
            cinteropArguments.linkerOptions.value.toTypedArray()
    val verbose = cinteropArguments.verbose

    val language = selectNativeLanguage(def.config)

    val entryPoint = def.config.entryPoints.atMostOne()
    val linkerOpts =
            def.config.linkerOpts.toTypedArray() + 
            tool.defaultCompilerOpts + 
            additionalLinkerOpts
    val linkerName = cinteropArguments.linker ?: def.config.linker
    val linker = "${tool.llvmHome}/bin/$linkerName"
    val compiler = "${tool.llvmHome}/bin/clang"
    val excludedFunctions = def.config.excludedFunctions.toSet()
    val excludedMacros = def.config.excludedMacros.toSet()
    val staticLibraries = def.config.staticLibraries + cinteropArguments.staticLibrary.toTypedArray()
    val libraryPaths = def.config.libraryPaths + cinteropArguments.libraryPath.toTypedArray()
    val fqParts = (cinteropArguments.pkg ?: def.config.packageName)?.split('.')
            ?: defFile!!.name.split('.').reversed().drop(1)

    val outKtFileName = fqParts.last() + ".kt"

    val outKtPkg = fqParts.joinToString(".")
    val outKtFileRelative = (fqParts + outKtFileName).joinToString("/")
    val outKtFile = File(ktGenRoot, outKtFileRelative)

    val libName = (additionalArgs["cstubsname"] as? String)?: fqParts.joinToString("") + "stubs"

    val tempFiles = TempFiles(libName, cinteropArguments.tempDir)

    val imports = parseImports((additionalArgs["import"] as? List<String>)?.toTypedArray() ?: arrayOf())

    val library = buildNativeLibrary(tool, def, cinteropArguments, imports)

    val (nativeIndex, compilation) = buildNativeIndex(library, verbose)

    // Our current approach to arm64_32 support is to compile armv7k version of bitcode
    // for arm64_32. That's the reason for this substitution.
    // TODO: Add proper support with the next LLVM update.
    val target = when (tool.target) {
        KonanTarget.WATCHOS_ARM64 -> KonanTarget.WATCHOS_ARM32
        else -> tool.target
    }
    val configuration = InteropConfiguration(
            library = compilation,
            pkgName = outKtPkg,
            excludedFunctions = excludedFunctions,
            excludedMacros = excludedMacros,
            strictEnums = def.config.strictEnums.toSet(),
            nonStrictEnums = def.config.nonStrictEnums.toSet(),
            noStringConversion = def.config.noStringConversion.toSet(),
            exportForwardDeclarations = def.config.exportForwardDeclarations,
            disableDesignatedInitializerChecks = def.config.disableDesignatedInitializerChecks,
            target = target
    )

    outKtFile.parentFile.mkdirs()

    File(nativeLibsDir).mkdirs()
    val outCFile = tempFiles.create(libName, ".${language.sourceFileExtension}")

    val logger = if (verbose) {
        { message: String -> println(message) }
    } else {
        {}
    }

    val stubIrContext = StubIrContext(logger, configuration, nativeIndex, imports, flavor, libName)
    val stubIrDriver = StubIrDriver(stubIrContext)
    stubIrDriver.run(outKtFile, File(outCFile.absolutePath), entryPoint)

    // TODO: if a library has partially included headers, then it shouldn't be used as a dependency.
    def.manifestAddendProperties["includedHeaders"] = nativeIndex.includedHeaders.joinToString(" ") { it.value }

    def.manifestAddendProperties.putAndRunOnReplace("package", outKtPkg) {
        _, oldValue, newValue ->
            warn("The package value `$oldValue` specified in .def file is overridden with explicit $newValue")
    }

    def.manifestAddendProperties["interop"] = "true"

    stubIrContext.addManifestProperties(def.manifestAddendProperties)

    manifestAddend?.parentFile?.mkdirs()
    manifestAddend?.let { def.manifestAddendProperties.storeProperties(it) }

    if (flavor == KotlinPlatform.JVM) {

        val outOFile = tempFiles.create(libName,".o")

        val compilerCmd = arrayOf(compiler, *stubIrContext.libraryForCStubs.compilerArgs.toTypedArray(),
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
        val compilerCmd = arrayOf(compiler, *stubIrContext.libraryForCStubs.compilerArgs.toTypedArray(),
                "-emit-llvm", "-c", outCFile.absolutePath, "-o", outLib.absolutePath)

        runCmd(compilerCmd, verbose)
    }
    return argsToCompiler(staticLibraries, libraryPaths)
}

internal fun prepareTool(target: String?, flavor: KotlinPlatform): ToolConfig {
    val tool = ToolConfig(target, flavor)
    tool.downloadDependencies()

    System.load(tool.libclang)

    return tool
}

internal fun buildNativeLibrary(
        tool: ToolConfig,
        def: DefFile,
        arguments: CInteropArguments,
        imports: ImportsImpl
): NativeLibrary {
    val additionalHeaders = (arguments.header).toTypedArray()
    val additionalCompilerOpts = (arguments.compilerOpts +
            arguments.compilerOptions + arguments.compilerOption).toTypedArray()

    val headerFiles = def.config.headers + additionalHeaders
    val language = selectNativeLanguage(def.config)
    val compilerOpts: List<String> = mutableListOf<String>().apply {
        addAll(def.config.compilerOpts)
        addAll(tool.defaultCompilerOpts)
        addAll(additionalCompilerOpts)
        addAll(getCompilerFlagsForVfsOverlay(arguments.headerFilterPrefix.toTypedArray(), def))
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

    val compilation = CompilationImpl(
            includes = headerFiles,
            additionalPreambleLines = def.defHeaderLines,
            compilerArgs = compilerOpts + tool.platformCompilerOpts,
            language = language
    )

    val headerFilter: NativeLibraryHeaderFilter
    val includes: List<String>

    val modules = def.config.modules

    if (modules.isEmpty()) {
        val excludeDependentModules = def.config.excludeDependentModules

        val headerFilterGlobs = def.config.headerFilter
        val headerInclusionPolicy = HeaderInclusionPolicyImpl(headerFilterGlobs)

        headerFilter = NativeLibraryHeaderFilter.NameBased(headerInclusionPolicy, excludeDependentModules)
        includes = headerFiles
    } else {
        require(language == Language.OBJECTIVE_C) { "cinterop supports 'modules' only when 'language = Objective-C'" }
        require(headerFiles.isEmpty()) { "cinterop doesn't support having headers and modules specified at the same time" }
        require(def.config.headerFilter.isEmpty()) { "cinterop doesn't support 'headerFilter' with 'modules'" }

        val modulesInfo = getModulesInfo(compilation, modules)

        headerFilter = NativeLibraryHeaderFilter.Predefined(modulesInfo.ownHeaders)
        includes = modulesInfo.topLevelHeaders
    }

    val excludeSystemLibs = def.config.excludeSystemLibs

    val headerExclusionPolicy = HeaderExclusionPolicyImpl(imports)

    return NativeLibrary(
            includes = includes,
            additionalPreambleLines = compilation.additionalPreambleLines,
            compilerArgs = compilation.compilerArgs,
            headerToIdMapper = HeaderToIdMapper(sysRoot = tool.sysRoot),
            language = compilation.language,
            excludeSystemLibs = excludeSystemLibs,
            headerExclusionPolicy = headerExclusionPolicy,
            headerFilter = headerFilter
    )
}
