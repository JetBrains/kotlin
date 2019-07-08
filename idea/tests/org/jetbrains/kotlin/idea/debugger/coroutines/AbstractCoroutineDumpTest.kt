/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutines

import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.jarRepository.JarRepositoryManager
import com.intellij.jarRepository.RemoteRepositoryDescription
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.idea.maven.aether.ArtifactKind
import org.jetbrains.jps.model.library.JpsMavenRepositoryLibraryDescriptor
import org.jetbrains.kotlin.idea.debugger.KotlinDebuggerTestBase
import org.jetbrains.kotlin.idea.debugger.evaluate.ExecutionContext
import java.io.File

abstract class AbstractCoroutineDumpTest : KotlinDebuggerTestBase() {


    protected fun doTest(path: String) {
        val fileText = FileUtil.loadFile(File(path))

        configureSettings(fileText)
        createAdditionalBreakpoints(fileText)
        createDebugProcess(path)

        doOnBreakpoint {
            val evalContext = EvaluationContextImpl(this, frameProxy)
            val execContext = ExecutionContext(evalContext, frameProxy ?: return@doOnBreakpoint)
            val either = CoroutinesDebugProbesProxy.dumpCoroutines(execContext)
            try {
                if (either.isRight)
                    try {
                        val states = either.get()
                        print(stringDump(states), ProcessOutputTypes.SYSTEM)
                    } catch (ignored: Throwable) {
                    }
                else
                    throw AssertionError("Dump failed", either.left)
            } finally {
                resume(this)
            }
        }

        doOnBreakpoint {
            val evalContext = EvaluationContextImpl(this, frameProxy)
            val execContext = ExecutionContext(evalContext, frameProxy ?: return@doOnBreakpoint)
            val either = CoroutinesDebugProbesProxy.dumpCoroutines(execContext)
            try {
                if (either.isRight)
                    try {
                        val states = either.get()
                        print(stringDump(states), ProcessOutputTypes.SYSTEM)
                    } catch (ignored: Throwable) {
                    }
                else
                    throw AssertionError("Dump failed", either.left)
            } finally {
                resume(this)
            }
        }
    }

    private fun stringDump(states: List<CoroutineState>) = buildString {
        states.forEach {
            appendln("\"${it.name}\", state: ${it.state}")
        }
    }

    override fun createJavaParameters(mainClass: String?): JavaParameters {
        val description = JpsMavenRepositoryLibraryDescriptor("org.jetbrains.kotlinx", "kotlinx-coroutines-debug", "1.3.0")
        val debugJar = JarRepositoryManager.loadDependenciesSync(
            project, description, setOf(ArtifactKind.ARTIFACT),
            RemoteRepositoryDescription.DEFAULT_REPOSITORIES, null
        ) ?: throw AssertionError("Debug Dependency is not found")
        val params = super.createJavaParameters(mainClass)
        for (jar in debugJar) {
            params.classPath.add(jar.file.presentableUrl)
            if (jar.file.name.contains("kotlinx-coroutines-debug"))
                params.vmParametersList.add("-javaagent:${jar.file.presentableUrl}")
        }
        return params
    }
}