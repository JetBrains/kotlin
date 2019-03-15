/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.extensions

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor

interface ScriptEvaluationExtension {
    companion object : ProjectExtensionDescriptor<ScriptEvaluationExtension>(
        "org.jetbrains.kotlin.scriptEvaluationExtension",
        ScriptEvaluationExtension::class.java
    )

    fun isAccepted(arguments: CommonCompilerArguments): Boolean

    // TODO: it would be nice to split KotlinCoreEnvironment to actual environment and compilation/project configuration
    fun eval(arguments: CommonCompilerArguments, coreEnvironment: KotlinCoreEnvironment): ExitCode
}
