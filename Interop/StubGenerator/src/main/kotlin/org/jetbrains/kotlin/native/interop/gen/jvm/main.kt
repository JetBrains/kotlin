package org.jetbrains.kotlin.native.interop.gen.jvm

import org.jetbrains.kotlin.native.interop.indexer.Language
import org.jetbrains.kotlin.native.interop.indexer.NativeIndex
import org.jetbrains.kotlin.native.interop.indexer.NativeLibrary
import org.jetbrains.kotlin.native.interop.indexer.buildNativeIndex
import java.io.File
import java.lang.IllegalArgumentException
import java.util.*

fun main(args: Array<String>) {
    val llvmInstallPath = System.getProperty("llvmInstallPath")!!

    val ktGenRoot = args[0]
    val nativeLibsDir = args[1]
    val otherArgs = args.drop(2)

    // TODO: remove OSX defaults.
    val substitutions = mapOf(
            "arch" to (System.getenv("TARGET_ARCH") ?: "x86-64"),
            "os" to (System.getenv("TARGET_OS") ?: detectHost())
    )

    processLib(ktGenRoot, nativeLibsDir, llvmInstallPath, substitutions, otherArgs)
}

private fun detectHost():String {
    val os =System.getProperty("os.name")
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

private fun getArgPrefix(arg: String): String? {
    val index = arg.indexOf(':')
    if (index == -1) {
        return null
    } else {
        return arg.substring(0, index)
    }
}

private fun dropPrefix(arg: String): String? {
    val index = arg.indexOf(':')
    if (index == -1) {
        return null
    } else {
        return arg.substring(index + 1)
    }
}

private fun ProcessBuilder.runExpectingSuccess() {
    println(this.command().joinToString(" "))

    val res = this.start().waitFor()
    if (res != 0) {
        throw Error("Process finished with non-zero exit code: $res")
    }
}

private fun Properties.getSpaceSeparated(name: String): List<String> {
    return this.getProperty(name)?.split(' ') ?: emptyList()
}

private fun List<String>?.isTrue(): Boolean {
    // The rightmost wins, null != "true".
    return this?.last() == "true"
}

private fun processLib(ktGenRoot: String,
                       nativeLibsDir: String,
                       llvmInstallPath: String,
                       substitutions: Map<String, String>,
                       additionalArgs: List<String>) {

    val args = additionalArgs.groupBy ({ getArgPrefix(it)!! }, { dropPrefix(it)!! }) // TODO

    val platformName = args["-target"]?.single() ?: "jvm"

    val platform = KotlinPlatform.values().single { it.name.equals(platformName, ignoreCase = true) }

    val defFile = args["-def"]?.single()?.let { File(it) }

    val config = Properties()
    defFile?.bufferedReader()?.use { reader ->
        config.load(reader)
    }
    substitute(config, substitutions)

    val additionalHeaders = args["-h"].orEmpty()
    val additionalCompilerOpts = args["-copt"].orEmpty()
    val additionalLinkerOpts = args["-lopt"].orEmpty()
    val generateShims = args["-shims"].isTrue()

    val headerFiles = config.getSpaceSeparated("headers") + additionalHeaders
    val compilerOpts = config.getSpaceSeparated("compilerOpts") + additionalCompilerOpts
    val compiler = "clang"
    val language = Language.C
    val linkerOpts = config.getSpaceSeparated("linkerOpts").toTypedArray() + additionalLinkerOpts
    val linker = args["-linker"]?.singleOrNull() ?: config.getProperty("linker") ?: "clang"
    val excludedFunctions = config.getSpaceSeparated("excludedFunctions").toSet()

    val fqParts = args["-pkg"]?.singleOrNull()?.let {
        it.split('.')
    } ?: defFile!!.name.split('.').reversed().drop(1)

    val outKtFileName = fqParts.last() + ".kt"

    val outKtPkg = fqParts.joinToString(".")
    val outKtFileRelative = (fqParts + outKtFileName).joinToString("/")
    val outKtFile = File(ktGenRoot, outKtFileRelative)

    val libName = fqParts.joinToString("") + "stubs"

    val nativeIndex = buildNativeIndex(NativeLibrary(headerFiles, compilerOpts, language))

    val gen = StubGenerator(nativeIndex, outKtPkg, libName, excludedFunctions, generateShims, platform)

    outKtFile.parentFile.mkdirs()
    outKtFile.bufferedWriter().use { out ->
        gen.withOutput({ out.appendln(it) }) {
            gen.generateKotlinFile()
        }
    }


    val outCFile = createTempFile(suffix = ".c")

    outCFile.bufferedWriter().use { out ->
        gen.withOutput({ out.appendln(it) }) {
            gen.generateCFile(headerFiles)
        }
    }

    File(nativeLibsDir).mkdirs()

    val workDir = defFile?.parentFile ?: File(System.getProperty("java.io.tmpdir"))

    if (platform == KotlinPlatform.JVM) {

        val outOFile = createTempFile(suffix = ".o")

        val javaHome = System.getProperty("java.home")
        val compilerArgsForJniIncludes = listOf("", "linux", "darwin").map { "-I$javaHome/../include/$it" }.toTypedArray()

        val compilerCmd = arrayOf("$llvmInstallPath/bin/$compiler", *compilerOpts.toTypedArray(),
                *compilerArgsForJniIncludes,
                "-c", outCFile.path, "-o", outOFile.path)

        ProcessBuilder(*compilerCmd)
                .directory(workDir)
                .inheritIO()
                .runExpectingSuccess()

        val outLib = nativeLibsDir + "/" + System.mapLibraryName(libName)

        val linkerCmd = arrayOf("$llvmInstallPath/bin/$linker", *linkerOpts, outOFile.path, "-shared", "-o", outLib,
                "-Wl,-flat_namespace,-undefined,dynamic_lookup")

        ProcessBuilder(*linkerCmd)
                .directory(workDir)
                .inheritIO()
                .runExpectingSuccess()

        outOFile.delete()
    } else if (platform == KotlinPlatform.NATIVE) {
        val outBcName = libName + ".bc"
        val outLib = nativeLibsDir + "/" + outBcName
        val compilerCmd = arrayOf("$llvmInstallPath/bin/$compiler", *compilerOpts.toTypedArray(),
                "-emit-llvm", "-c", outCFile.path, "-o", outLib)

        ProcessBuilder(*compilerCmd)
                .directory(workDir)
                .inheritIO()
                .runExpectingSuccess()
    }

    outCFile.delete()
}
