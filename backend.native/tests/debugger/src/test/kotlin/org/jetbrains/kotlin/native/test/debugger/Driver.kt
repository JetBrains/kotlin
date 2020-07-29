/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.native.test.debugger

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.cli.bc.K2Native
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit


class ToolDriver(
        private val useInProcessCompiler: Boolean = false
) {
    fun compile(source: Path, output: Path, vararg args: String) {
        check(!Files.exists(output))
        val allArgs = listOf("-output", output.toString(), source.toString(), *args).toTypedArray()

        if (useInProcessCompiler) {
            K2Native.main(allArgs)
        } else {
            subprocess(DistProperties.konanc, *allArgs).thrownIfFailed()
        }
        check(Files.exists(output)) {
            "Compiler has not produced an output at $output"
        }
    }

    fun cinterop(defFile:Path, output: Path, pkg: String, vararg args: String) {
        val allArgs = listOf("-o", output.toString(), "-def", defFile.toString(), "-pkg", pkg, *args).toTypedArray()

        //TODO: do we need in process cinterop?
        subprocess(DistProperties.cinterop, *allArgs).thrownIfFailed()
        check(Files.exists(output)) {
            "Compiler has not produced an output at $output"
        }
    }

    fun runLldb(program: Path, commands: List<String>): String {
        val args = listOf("-b", "-o", "command script import \"${DistProperties.lldbPrettyPrinters}\"") +
                commands.flatMap { listOf("-o", it) }
        return subprocess(DistProperties.lldb, program.toString(), "-b", *args.toTypedArray())
                .thrownIfFailed()
                .stdout
    }

    fun runDwarfDump(program: Path, vararg args:String = emptyArray(), processor:List<DwarfTag>.()->Unit) {
        val dwarfProcess = subprocess(DistProperties.dwarfDump, *args, "${program}.dSYM/Contents/Resources/DWARF/${program.fileName}")
        val out = dwarfProcess.takeIf { it.process.exitValue() == 0 }?.stdout ?: error(dwarfProcess.stderr)
        DwarfUtilParser().parse(StringReader(out)).tags.toList().processor()
    }
}

data class ProcessOutput(
        val program: Path,
        val process: Process,
        val stdout: String,
        val stderr: String,
        val durationMs: Long
) {
    fun thrownIfFailed(): ProcessOutput {
        fun renderStdStream(name: String, text: String): String =
                if (text.isBlank()) "$name is empty" else "$name:\n$text"

        check(process.exitValue() == 0) {
            """$program exited with non-zero value: ${process.exitValue()}
              |${renderStdStream("stdout", stdout)}
              |${renderStdStream("stderr", stderr)}
            """.trimMargin()
        }
        return this
    }
}

fun subprocess(program: Path, vararg args: String): ProcessOutput {
    val start = System.currentTimeMillis()
    val process = ProcessBuilder(program.toString(), *args).start()
    val out = GlobalScope.async(Dispatchers.IO) {
        readStream(process, process.inputStream.buffered())
    }

    val err = GlobalScope.async(Dispatchers.IO) {
        readStream(process, process.errorStream.buffered())
    }

    return runBlocking {
        try {
            val status = process.waitFor(5L, TimeUnit.MINUTES)
            if (!status) {
                out.cancel()
                err.cancel()
                error("$program timeouted")
            }
        }catch (e:Exception) {
            out.cancel()
            err.cancel()
            error(e)
        }
        ProcessOutput(program, process, out.await(), err.await(), System.currentTimeMillis() - start)
    }
}

private fun readStream(process: Process, stream: InputStream): String {
    var size = 4096
    val buffer = ByteArray(size) { 0 }
    val sunk = ByteArrayOutputStream()
    while (true) {
        size = stream.read(buffer, 0, buffer.size)
        if (size < 0 && !process.isAlive)
            break
        if (size > 0)
            sunk.write(buffer, 0, size)
    }
    return String(sunk.toByteArray())
}
