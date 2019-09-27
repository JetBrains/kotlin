/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package org.jetbrains.kotlin

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.konan.target.Architecture

import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.Xcode

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * A replacement of the standard `exec {}`
 * @see org.gradle.api.Project.exec
 */
interface ExecutorService {
    fun execute(closure: Closure<in ExecSpec>): ExecResult? = execute(ConfigureUtil.configureUsing(closure))
    fun execute(action: Action<in ExecSpec>): ExecResult?
}

/**
 * Creates an ExecutorService depending on a test target -Ptest_target
 */
fun create(project: Project): ExecutorService {
    val platformManager = project.platformManager
    val testTarget = project.testTarget
    val platform = platformManager.platform(testTarget)
    val absoluteTargetToolchain = platform.absoluteTargetToolchain
    val absoluteTargetSysRoot = platform.absoluteTargetSysRoot

    return when (testTarget) {
        KonanTarget.WASM32 -> object : ExecutorService {
            override fun execute(action: Action<in ExecSpec>): ExecResult? = project.exec { execSpec ->
                action.execute(execSpec)
                with(execSpec) {
                    val exe = executable
                    val d8 = "$absoluteTargetToolchain/bin/d8"
                    val launcherJs = "$executable.js"
                    executable = d8
                    args = listOf("--expose-wasm", launcherJs, "--", exe) + args
                }
            }
        }

        KonanTarget.LINUX_MIPS32, KonanTarget.LINUX_MIPSEL32 -> object : ExecutorService {
            override fun execute(action: Action<in ExecSpec>): ExecResult? = project.exec { execSpec ->
                action.execute(execSpec)
                with(execSpec) {
                    val qemu = if (platform.target === KonanTarget.LINUX_MIPS32) "qemu-mips" else "qemu-mipsel"
                    val absoluteQemu = "$absoluteTargetToolchain/bin/$qemu"
                    val exe = executable
                    executable = absoluteQemu
                    args = listOf("-L", absoluteTargetSysRoot,
                        // This is to workaround an endianess issue.
                        // See https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=731082 for details.
                        "$absoluteTargetSysRoot/lib/ld.so.1", "--inhibit-cache",
                        exe) + args
                }
            }
        }

        KonanTarget.IOS_X64,
        KonanTarget.TVOS_X64,
        KonanTarget.WATCHOS_X86,
        KonanTarget.WATCHOS_X64 -> simulator(project)

        else -> {
            if (project.hasProperty("remote")) sshExecutor(project)
            else object : ExecutorService {
                override fun execute(action: Action<in ExecSpec>): ExecResult? = project.exec(action)
            }
        }
    }
}

data class ProcessOutput(var stdOut: String, var stdErr: String, var exitCode: Int)

/**
 * Runs process using a given executor.
 *
 * @param executor a method that is able to run a given executable, e.g. ExecutorService::execute
 * @param executable a process executable to be run
 * @param args arguments for a process
 */
fun runProcess(executor: (Action<in ExecSpec>) -> ExecResult?,
               executable: String, args: List<String>) : ProcessOutput {
    val outStream = ByteArrayOutputStream()
    val errStream = ByteArrayOutputStream()

    val execResult = executor(Action {
        it.executable = executable
        it.args = args.toList()
        it.standardOutput = outStream
        it.errorOutput = errStream
        it.isIgnoreExitValue = true
    })

    checkNotNull(execResult)

    val stdOut = outStream.toString("UTF-8")
    val stdErr = errStream.toString("UTF-8")

    return ProcessOutput(stdOut, stdErr, execResult.exitValue)
}

fun runProcess(executor: (Action<in ExecSpec>) -> ExecResult?,
               executable: String, vararg args: String) = runProcess(executor, executable, args.toList())

/**
 * Runs process using a given executor.
 *
 * @param executor a method that is able to run a given executable, e.g. ExecutorService::execute
 * @param executable a process executable to be run
 * @param args arguments for a process
 * @param input an input string to be passed through the standard input stream
 */
fun runProcessWithInput(executor: (Action<in ExecSpec>) -> ExecResult?,
               executable: String, args: List<String>, input: String) : ProcessOutput {
    val outStream = ByteArrayOutputStream()
    val errStream = ByteArrayOutputStream()
    val inStream = ByteArrayInputStream(input.toByteArray())

    val execResult = executor(Action {
        it.executable = executable
        it.args = args.toList()
        it.standardOutput = outStream
        it.errorOutput = errStream
        it.isIgnoreExitValue = true
        it.standardInput = inStream
    })

    checkNotNull(execResult)

    val stdOut = outStream.toString("UTF-8")
    val stdErr = errStream.toString("UTF-8")

    return ProcessOutput(stdOut, stdErr, execResult.exitValue)
}

/**
 * The [ExecutorService] being set in the given project.
 * @throws IllegalStateException if there are no executor in the project.
 */
val Project.executor: ExecutorService
    get() = this.convention.plugins["executor"] as? ExecutorService ?: throw IllegalStateException("Executor wasn't found")

/**
 * Creates a new executor service with additional action [actionParameter] executed after the main one.
 * The following is an example how to pass an environment parameter
 * @code `executor.add(Action { it.environment = mapOf("JAVA_OPTS" to "-verbose:gc") })::execute`
 */
fun ExecutorService.add(actionParameter: Action<in ExecSpec>) = object : ExecutorService {
    override fun execute(action: Action<in ExecSpec>): ExecResult? =
            this@add.execute(Action {
                action.execute(it)
                actionParameter.execute(it)
            })
}

/**
 * Executes the [executable] with the given [arguments]
 * and checks that the program finished with zero exit code.
 */
fun Project.executeAndCheck(executable: Path, arguments: List<String> = emptyList()) {
    val (stdOut, stdErr, exitCode) = runProcess(
            executor = executor::execute,
            executable = executable.toString(),
            args = arguments
    )

    println("""
            |stdout: $stdOut
            |stderr: $stdErr
            """.trimMargin())
    check(exitCode == 0) { "Execution failed with exit code: $exitCode "}
}

/**
 * Returns [project]'s process executor.
 * @see Project.exec
 */
fun localExecutor(project: Project) = { a: Action<in ExecSpec> -> project.exec(a) }

/**
 * Executes a given action with iPhone Simulator.
 *
 * The test target should be specified with -Ptest_target=ios_x64
 * @see KonanTarget.IOS_X64
 * @param iosDevice an optional project property used to control simulator's device type
 *        Specify -PiosDevice=iPhone X to set it
 */
private fun simulator(project: Project) : ExecutorService = object : ExecutorService {

    private val target = project.testTarget

    private val simctl by lazy {
        val sdk = when (target) {
            KonanTarget.TVOS_X64 -> Xcode.current.appletvsimulatorSdk
            KonanTarget.IOS_X64 -> Xcode.current.iphonesimulatorSdk
            KonanTarget.WATCHOS_X64,
            KonanTarget.WATCHOS_X86 -> Xcode.current.watchsimulatorSdk
            else -> error("Unexpected simulation target: $target")
        }
        val out = ByteArrayOutputStream()
        val result = project.exec {
            it.commandLine("/usr/bin/xcrun", "--find", "simctl", "--sdk", sdk)
            it.standardOutput = out
        }
        result.assertNormalExitValue()
        out.toString("UTF-8").trim()
    }

    private val device = project.findProperty("iosDevice")?.toString() ?: when (target) {
        KonanTarget.TVOS_X64 -> "Apple TV 4K"
        KonanTarget.IOS_X64 -> "iPhone 8"
        KonanTarget.WATCHOS_X64,
        KonanTarget.WATCHOS_X86 -> "Apple Watch Series 4 - 40mm"
        else -> error("Unexpected simulation target: $target")
    }

    private val archSpecification = when (target.architecture) {
        Architecture.X86 -> listOf("-a", "i386")
        Architecture.X64 -> listOf() // x86-64 is used by default.
        else -> error("${target.architecture} can't be used in simulator.")
    }.toTypedArray()

    override fun execute(action: Action<in ExecSpec>): ExecResult? = project.exec { execSpec ->
        action.execute(execSpec)
        // Starting Xcode 11 `simctl spawn` requires explicit `--standalone` flag.
        with(execSpec) { commandLine = listOf(simctl, "spawn", "--standalone", *archSpecification, device, executable) + args }
    }
}

/**
 * Remote process executor.
 *
 * @param remote makes binaries be executed on a remote host
 *        Specify it as -Premote=user@host
 */
private fun sshExecutor(project: Project) : ExecutorService = object : ExecutorService {

    private val remote: String = project.property("remote").toString()
    private val sshArgs: List<String> = System.getenv("SSH_ARGS")?.split(" ") ?: emptyList()
    private val sshHome = System.getenv("SSH_HOME") ?: "/usr/bin"

    // Unique remote dir name to be used in the target host
    private val remoteDir = run {
        val date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
        Paths.get(project.findProperty("remoteRoot").toString(), "tmp",
                System.getProperty("user.name") + "_" + date).toString()
    }

    override fun execute(action: Action<in ExecSpec>): ExecResult {
        var execFile: String? = null

        createRemoteDir()
        val execResult = project.exec { execSpec ->
            action.execute(execSpec)
            with(execSpec) {
                upload(executable)
                executable = "$remoteDir/${File(executable).name}"
                execFile = executable
                commandLine = arrayListOf("$sshHome/ssh") + sshArgs + remote + commandLine
            }
        }
        cleanup(execFile!!)
        return execResult
    }

    private fun createRemoteDir() {
        project.exec {
            it.commandLine = arrayListOf("$sshHome/ssh") + sshArgs + remote + "mkdir" + "-p" + remoteDir
        }
    }

    private fun upload(fileName: String) {
        project.exec {
            it.commandLine = arrayListOf("$sshHome/scp") + sshArgs + fileName + "$remote:$remoteDir"
        }
    }

    private fun cleanup(fileName: String) {
        project.exec {
            it.commandLine = arrayListOf("$sshHome/ssh") + sshArgs + remote + "rm" + fileName
        }
    }
}
