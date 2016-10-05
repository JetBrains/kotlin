package org.jetbrains.kotlin.native.interop.gen.jvm

import kotlin_native.interop.Ref.to
import org.jetbrains.kotlin.native.interop.indexer.NativeIndex
import org.jetbrains.kotlin.native.interop.indexer.buildNativeIndex
import java.io.File
import java.lang.IllegalArgumentException
import java.util.*
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val llvmInstallPath = System.getProperty("llvmInstallPath")!!

    val ktGenRoot = args[0]
    val nativeLibsDir = args[1]
    val ktSrcRoots = args.drop(2)
    // TODO: remove OSX defaults.
    val substitutions = mapOf(
            "arch" to (System.getenv("TARGET_ARCH") ?: "x86-64"),
            "os" to (System.getenv("TARGET_OS") ?: detectHost())
    )


    ktSrcRoots.forEach { ktSrcRoot ->
        val defFiles = File(ktSrcRoot).walk().filter { it.name.endsWith(".def") }

        defFiles.forEach { defFile ->
            processDefFile(ktSrcRoot, defFile, ktGenRoot, nativeLibsDir, llvmInstallPath, substitutions)
        }
    }
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

private fun processDefFile(ktSrcRoot: String, defFile: File, ktGenRoot: String, nativeLibsDir: String, llvmInstallPath: String, substitutions: Map<String, String>) {
    val config = Properties()
    defFile.bufferedReader().use { reader ->
        config.load(reader)
    }
    substitute(config, substitutions)

    val headerFiles = config.getProperty("headers").split(' ')
    val compilerOpts = config.getProperty("compilerOpts").split(' ')
    val compiler = config.getProperty("compiler")
    val libName = config.getProperty("libName")
    val linkerOpts = config.getProperty("linkerOpts").split(' ').toTypedArray()
    val linker = config.getProperty("linker")
    val excludedFunctions = config.getProperty("excludedFunctions")?.split(' ')?.toSet() ?: emptySet()


    val defFileRelative = defFile.relativeTo(File(ktSrcRoot))
    val outKtFile = File(ktGenRoot, defFileRelative.toString().substringBeforeLast(".def") + ".kt")
    val outKtPkg = defFileRelative.parentFile?.path?.replace(File.separatorChar, '.') ?: ""


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

    println(compilerCmd.joinToString(" "))

    val compilerRes = ProcessBuilder(*compilerCmd)
            .inheritIO()
            .start()
            .waitFor()

    if (compilerRes != 0) {
        exitProcess(compilerRes)
    }

    File(nativeLibsDir).mkdirs()

    val outLib = nativeLibsDir + "/" + System.mapLibraryName(libName)

    val linkerCmd = arrayOf("$llvmInstallPath/bin/$linker", *linkerOpts, outOFile.path, "-shared", "-o", outLib,
            "-Wl,-flat_namespace,-undefined,dynamic_lookup")

    println(linkerCmd.joinToString(" "))

    val linkerRes = ProcessBuilder(*linkerCmd)
            .inheritIO()
            .start()
            .waitFor()

    if (linkerRes != 0) {
        exitProcess(linkerRes)
    }

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