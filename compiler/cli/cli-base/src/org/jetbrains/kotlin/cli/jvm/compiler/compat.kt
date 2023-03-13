/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger

object IdeaStandaloneExecutionSetup {
    private val LOG: Logger = Logger.getInstance(IdeaStandaloneExecutionSetup::class.java)
    // Copy-pasted from com.intellij.openapi.util.BuildNumber#FALLBACK_VERSION
    private const val FALLBACK_IDEA_BUILD_NUMBER = "999.SNAPSHOT"

    fun doSetup() {
        checkInHeadlessMode()

        System.getProperties().setProperty("project.structure.add.tools.jar.to.new.jdk", "false")
        System.getProperties().setProperty("psi.track.invalidation", "true")
        System.getProperties().setProperty("psi.incremental.reparse.depth.limit", "1000")
        System.getProperties().setProperty("ide.hide.excluded.files", "false")
        System.getProperties().setProperty("ast.loading.filter", "false")
        System.getProperties().setProperty("idea.ignore.disabled.plugins", "true")
        // Setting the build number explicitly avoids the command-line compiler
        // reading /tmp/build.txt in an attempt to get a build number from there.
        // See intellij platform PluginManagerCore.getBuildNumber.
        System.getProperties().setProperty("idea.plugins.compatible.build", FALLBACK_IDEA_BUILD_NUMBER)
    }

    private fun checkInHeadlessMode() {
        // if  application is null it means that we are in progress of set-up application environment i.e. we are not in the running IDEA
        val application = ApplicationManager.getApplication() ?: return
        if (!application.isHeadlessEnvironment) {
            LOG.error(Throwable("IdeaStandaloneExecutionSetup should be called only in headless environment"))
        }
    }
}

// We safe this function to minimize the patch for release branches
fun setupIdeaStandaloneExecution() = IdeaStandaloneExecutionSetup.doSetup()
