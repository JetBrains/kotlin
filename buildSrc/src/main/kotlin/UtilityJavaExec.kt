/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

import org.apache.tools.ant.types.Commandline
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.process.JavaExecSpec
import org.gradle.process.internal.ExecActionFactory
import org.gradle.process.internal.JavaExecAction
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import javax.inject.Inject

/**
 * This task does the same as original JavaExec task
 * We avoid using JavaExec tasks due to IDEA-200192:
 * IDEA makes all JavaExec tasks not up-to-date and attaches debugger making our breakpoints trigger during irrelevant task execution
 */
open class UtilityJavaExec(
    private val javaExecAction: JavaExecAction
) : ConventionTask(), JavaExecSpec by javaExecAction {

    @Inject
    constructor(execActionFactory: ExecActionFactory) : this(execActionFactory.newJavaExecAction())

    @TaskAction
    fun exec() {
        // like in original JavaExec task
        main = main // make convention mapping work (at least for 'main'...
        jvmArgs = jvmArgs  // ...and for 'jvmArgs')

        javaExecAction.execute()
    }


    @Option(
        option = "debug-jvm",
        description = "Enable debugging for the process. The process is started suspended and listening on port 5005. [INCUBATING]"
    )
    override fun setDebug(enabled: Boolean) {
        javaExecAction.debug = enabled
    }


    @Option(option = "args", description = "Command line arguments passed to the main class. [INCUBATING]")
    fun setArgsString(args: String): JavaExecSpec {
        return setArgs(Arrays.asList(*Commandline.translateCommandline(args)))
    }

    @Optional
    @Input
    override fun getExecutable(): String? = javaExecAction.executable

    @Internal
    override fun getWorkingDir(): File = javaExecAction.workingDir

    @Internal
    override fun getEnvironment(): Map<String, Any> = javaExecAction.environment

    @Internal
    override fun getStandardInput(): InputStream = javaExecAction.standardInput

    @Internal
    override fun getStandardOutput(): OutputStream = javaExecAction.standardOutput

    @Internal
    override fun getErrorOutput(): OutputStream = javaExecAction.errorOutput

    @Input
    override fun isIgnoreExitValue(): Boolean = javaExecAction.isIgnoreExitValue

    @Internal
    override fun getCommandLine(): List<String> = javaExecAction.commandLine
}