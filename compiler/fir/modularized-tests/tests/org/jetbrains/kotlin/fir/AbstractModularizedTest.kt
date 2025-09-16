/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.types.AbstractTypeChecker
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

abstract class AbstractModularizedTest(val config: ModularizedTestConfig) {

    val isUnderTeamcity: Boolean = System.getenv("TEAMCITY_VERSION") != null

    private val folderDateFormat = SimpleDateFormat("yyyy-MM-dd")
    private lateinit var reportDate: Date

    protected fun reportDir() = File(FIR_LOGS_PATH, folderDateFormat.format(reportDate))
        .also {
            it.mkdirs()
        }

    protected val reportDateStr: String by lazy {
        val reportDateFormat = SimpleDateFormat("yyyy-MM-dd__HH-mm")
        reportDateFormat.format(reportDate)
    }

    private fun detectReportDate(): Date {
        return config.reportTimestamp?.let { Date(it) } ?: Date()
    }

    open fun setUp() {
        AbstractTypeChecker.RUN_SLOW_ASSERTIONS = config.enableSlowAssertions
        reportDate = detectReportDate()
    }

    open fun tearDown() {
        AbstractTypeChecker.RUN_SLOW_ASSERTIONS = true
    }

    protected abstract fun beforePass(pass: Int)
    protected abstract fun afterPass(pass: Int)
    protected open fun afterAllPasses() {}
    protected abstract fun processModule(moduleData: ModuleData): ProcessorAction

    protected fun runTestOnce(pass: Int) {
        beforePass(pass)
        val testDataPath = config.jpsDir ?: "/Users/jetbrains/jps"
        val root = File(testDataPath)

        println("BASE PATH: ${root.absolutePath}")

        val additionalMessages = mutableListOf<String>()

        val filterRegex = config.outputDirRegexFilter.toRegex()
        val moduleName = config.moduleNameFilter
        val moduleNameRegexOutFilter = config.moduleNameRegexOut?.toRegex()
        val containsSourcesFilter = config.containsSourcesRegexFilter?.toRegex()
        val files = root.listFiles() ?: emptyArray()
        val modules = files.filter {
            it.extension == "xml" && (moduleNameRegexOutFilter == null || !it.name.matches(moduleNameRegexOutFilter))
        }
            .sortedBy { it.lastModified() }
            .flatMap { loadModuleDumpFile(it, config) }
            .sortedBy { it.timestamp }
            .also { additionalMessages += "Discovered ${it.size} modules" }
            .filter { it.rawOutputDir.matches(filterRegex) }
            .also { additionalMessages += "Filtered by regex to ${it.size} modules" }
            .let { modules ->
                if (containsSourcesFilter != null) {
                    modules.filter { module ->
                        module.rawSources.any { s -> containsSourcesFilter.containsMatchIn(s) }
                    }.also { additionalMessages += "Filtered by source paths to ${it.size} modules" }
                } else modules
            }
            .filter { (moduleName == null) || it.name == moduleName }
            .also { additionalMessages += "Filtered by module name to ${it.size} modules" }
            .filter { !it.isCommon }
            .also { additionalMessages += "Filtered by common flag to ${it.size} modules" }

        if (modules.isEmpty() && isUnderTeamcity) {
            println("------------------------ Flakiness diagnostic ------------------------")
            println("No modules found for pattern `${config.outputDirRegexFilter}` in `$testDataPath`")
            println("TestData root exists: ${root.exists()}")
            println(files.joinToString(prefix = "Content of testdata root:\n", separator = "\n") { it.absolutePath })
            println()
            additionalMessages.forEach { println(it) }
            println("------------------------------------------------")
        }


        for (module in modules.progress(step = 0.0) { "Analyzing ${it.qualifiedName}" }) {
            if (processModule(module).stop()) {
                break
            }
        }

        afterPass(pass)
    }
}

