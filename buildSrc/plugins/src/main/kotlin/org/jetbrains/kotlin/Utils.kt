package org.jetbrains.kotlin

import org.gradle.api.Project
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.PlatformManager

fun Project.platformManager() = rootProject.findProperty("platformManager") as PlatformManager

fun Project.testTarget() = platformManager().targetManager(project.findProperty("testTarget") as String?).target

/**
 * Ad-hoc signing of the specified path
 */
fun codesign(project: Project, path: String) {
    check(HostManager.hostIsMac, { "Apple specific code signing" } )
    val (stdOut, stdErr, exitCode) = runProcess(executor = localExecutor(project), executable = "/usr/bin/codesign",
            args = listOf("--verbose", "-s", "-", path))
    check(exitCode == 0, { """
            |Codesign failed with exitCode: $exitCode
            |stdout: $stdOut
            |stderr: $stdErr
            """.trimMargin()
    })
}