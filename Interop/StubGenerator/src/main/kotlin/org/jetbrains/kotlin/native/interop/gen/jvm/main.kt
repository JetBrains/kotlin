package org.jetbrains.kotlin.native.interop.gen.jvm

import org.jetbrains.kotlin.native.interop.indexer.NativeIndex
import org.jetbrains.kotlin.native.interop.indexer.buildNativeIndex
import java.io.File
import java.lang.IllegalArgumentException
import java.util.*

fun main(args: Array<String>) {
    val llvmInstallPath = System.getProperty("llvmInstallPath")!!

    val ktGenRoot = args[0]
    val nativeLibsDir = args[1]
    val defFile = args[2]
    val otherArgs = args.drop(3)

    // TODO: remove OSX defaults.
    val substitutions = mapOf(
            "arch" to (System.getenv("TARGET_ARCH") ?: "x86-64"),
            "os" to (System.getenv("TARGET_OS") ?: detectHost())
    )

    processDefFile(File(defFile), ktGenRoot, nativeLibsDir, llvmInstallPath, substitutions, otherArgs)
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

private fun String.removePrefixOrNull(prefix: String): String? {
    if (this.startsWith(prefix)) {
        return this.substring(prefix.length)
    } else {
        return null
    }
}

private fun ProcessBuilder.runExpectingSuccess() {
    println(this.command().joinToString(" "))

    val res = this.start().waitFor()
    if (res != 0) {
        throw Error("Process finished with non-zero exit code: $res")
    }
}

private fun processDefFile(defFile: File,
                           ktGenRoot: String,
                           nativeLibsDir: String,
                           llvmInstallPath: String,
                           substitutions: Map<String, String>,
                           additionalArgs: List<String>) {

    val config = Properties()
    defFile.bufferedReader().use { reader ->
        config.load(reader)
    }
    substitute(config, substitutions)

    val additionalCompilerOpts = additionalArgs.mapNotNull { it.removePrefixOrNull("-copt:") }
    val additionalLinkerOpts = additionalArgs.mapNotNull { it.removePrefixOrNull("-lopt:") }

    val headerFiles = config.getProperty("headers").split(' ')
    val compilerOpts = config.getProperty("compilerOpts").split(' ') + additionalCompilerOpts
    val compiler = config.getProperty("compiler")
    val linkerOpts = config.getProperty("linkerOpts").split(' ').toTypedArray() + additionalLinkerOpts
    val linker = config.getProperty("linker")
    val excludedFunctions = config.getProperty("excludedFunctions")?.split(' ')?.toSet() ?: emptySet()

    val fqParts = defFile.name.split('.').reversed().drop(1)

    val outKtFileName = fqParts.last() + ".kt"

    val outKtPkg = fqParts.joinToString(".")
    val outKtFileRelative = (fqParts + outKtFileName).joinToString("/")
    val outKtFile = File(ktGenRoot, outKtFileRelative)

    val libName = fqParts.joinToString("") + "stubs"

    val nativeIndex = buildNativeIndex(headerFiles, compilerOpts)

    val gen = StubGenerator(nativeIndex, outKtPkg, libName, excludedFunctions)

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

    val outOFile = createTempFile(suffix = ".o")

    val javaHome = System.getProperty("java.home")
    val compilerArgsForJniIncludes = listOf("", "linux", "darwin").map { "-I$javaHome/../include/$it" }.toTypedArray()

    val compilerCmd = arrayOf("$llvmInstallPath/bin/$compiler", *compilerOpts.toTypedArray(),
            *compilerArgsForJniIncludes,
            "-c", outCFile.path, "-o", outOFile.path)

    val defFileDir = defFile.parentFile

    ProcessBuilder(*compilerCmd)
            .directory(defFileDir)
            .inheritIO()
            .runExpectingSuccess()

    File(nativeLibsDir).mkdirs()

    val outLib = nativeLibsDir + "/" + System.mapLibraryName(libName)

    val linkerCmd = arrayOf("$llvmInstallPath/bin/$linker", *linkerOpts, outOFile.path, "-shared", "-o", outLib,
            "-Wl,-flat_namespace,-undefined,dynamic_lookup")

    ProcessBuilder(*linkerCmd)
            .directory(defFileDir)
            .inheritIO()
            .runExpectingSuccess()

    outCFile.delete()
    outOFile.delete()
}

private fun buildNativeIndex(headerFiles: List<String>, compilerOpts: List<String>): NativeIndex {
    val tempHeaderFile = createTempFile(suffix = ".h")
    tempHeaderFile.deleteOnExit()
    tempHeaderFile.writer().buffered().use { reader ->
        headerFiles.forEach {
            reader.appendln("#include <$it>")
        }
    }

    val res = buildNativeIndex(tempHeaderFile, compilerOpts)
    println(res)
    return res
}