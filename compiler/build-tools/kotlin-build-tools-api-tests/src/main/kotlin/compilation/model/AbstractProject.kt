/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation.model

import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createDirectories
import kotlin.io.path.isDirectory

interface Project {
    val kotlinToolchain: KotlinToolchains
    val defaultStrategyConfig: ExecutionPolicy
    val projectDirectory: Path
}

abstract class AbstractProject<out O : BaseCompilationOperation, B : BaseCompilationOperation.Builder, IC : BaseIncrementalCompilationConfiguration.Builder>(
    override val kotlinToolchain: KotlinToolchains,
    override val defaultStrategyConfig: ExecutionPolicy,
    override val projectDirectory: Path,
) : Project, AutoCloseable {
    protected val invalidModuleNameCharactersRegex = """[\\/\r\n\t]""".toRegex()
    protected val kotlinBuild = kotlinToolchain.createBuildSession()

    abstract fun module(
        moduleName: String,
        dependencies: List<Dependency> = emptyList(),
        snapshotConfig: SnapshotConfig = SnapshotConfig(ClassSnapshotGranularity.CLASS_MEMBER_LEVEL, true),
        stdlibClasspath: List<Path>? = null,
        moduleCompilationConfigAction: (B) -> Unit = {},
    ): Module<O, B, IC>

    protected fun initModule(module: AbstractModule<in O, B, IC>, moduleName: String) {
        module.sourcesDirectory.createDirectories()
        val templatePath = Paths.get(System.getProperty("kotlin.test.templates.classpath") + "/modules/$moduleName")
        assert(templatePath.isDirectory()) {
            "Template for $moduleName not found. Expected template directory path is $templatePath"
        }
        templatePath.copyToRecursively(module.sourcesDirectory, followLinks = false)
    }

    override fun close() {
        kotlinBuild.close()
    }
}
