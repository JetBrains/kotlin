/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.tasks.JavaExec

/**
 * Workaround for IDEA-200192:
 * IDEA makes all JavaExec tasks not up-to-date and attaches debugger making our breakpoints trigger during irrelevant task execution
 */
open class NoDebugJavaExec : JavaExec() {
    private fun String.isDebuggerArgument(): Boolean =
        startsWith("-agentlib:jdwp=transport=dt_socket,server=n,suspend=y,address=")

    override fun setJvmArgs(arguments: MutableList<String>?) {
        super.setJvmArgs(arguments?.filterNot { it.isDebuggerArgument() })
    }
}