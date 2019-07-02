package org.jetbrains.kotlin

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import org.gradle.util.ConfigureUtil

internal class ExecLlvm(private val project: Project) {

    fun execLlvm(tool: String, closure: Closure<in ExecSpec>): ExecResult =
            this.execLlvm(tool, ConfigureUtil.configureUsing(closure))

    fun execLlvm(tool: String, action: Action<in ExecSpec>): ExecResult {
        val extendedAction = Action<ExecSpec> { execSpec ->
            action.execute(execSpec)

            execSpec.apply {
                if (executable == null) {
                    val llvmDir = project.findProperty("llvmDir")
                    executable = "$llvmDir/bin/$tool"
                }
            }
        }
        return project.exec(extendedAction)
    }
}