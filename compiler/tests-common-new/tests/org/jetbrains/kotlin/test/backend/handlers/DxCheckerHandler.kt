/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.codegen.D8Checker
import org.jetbrains.kotlin.codegen.getClassFiles
import org.jetbrains.kotlin.generators.util.GeneratorsFileUtil
import org.jetbrains.kotlin.test.backend.codegenSuppressionChecker
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_DEXING
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.RUN_DEX_CHECKER
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.jvm.compiledClassesManager

class DxCheckerHandler(testServices: TestServices) : JvmBinaryArtifactHandler(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives)

    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {
        if (RUN_DEX_CHECKER !in module.directives || IGNORE_DEXING in module.directives) return
        val compiledClassesManager = testServices.compiledClassesManager
        try {
            D8Checker.check(info.classFileFactory)
        } catch (e: Throwable) {
            if (!GeneratorsFileUtil.isTeamCityBuild &&
                !testServices.codegenSuppressionChecker.failuresInModuleAreIgnored(module)
            ) {
                try {
                    val javaDir = compiledClassesManager.getCompiledJavaDirForModule(module)
                    if (javaDir != null) {
                        println("Compiled Java files: ${javaDir.absolutePath}")
                    }
                    val kotlinDir = compiledClassesManager.getCompiledKotlinDirForModule(module)
                    println("Compiled Kotlin files: ${kotlinDir.absolutePath}")
                    info.classFileFactory.getClassFiles().forEach {
                        println(" * ${it.relativePath}")
                    }
                    println(info.classFileFactory.createText())
                } catch (_: Throwable) {
                    // In FIR we have factory which can't print bytecode
                    //   and it throws exception otherwise. So we need
                    //   ignore that exception to report original one
                    // TODO: fix original problem
                }
            }
            throw e
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}
