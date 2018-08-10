/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec

import java.io.File

abstract class SpecTestsStatElement {
    var counter = 0
    abstract val elements: MutableMap<*, out SpecTestsStatElement>?
    open fun increment() {
        counter++
    }
}

class SpecTestsTypeStat(private val paragraph: SpecTestsStatElement) : SpecTestsStatElement() {
    override val elements = null
    override fun increment() {
        super.increment()
        paragraph.increment()
    }
}

class SpecTestsParagraphStat(private val section: SpecTestsStatElement) : SpecTestsStatElement() {
    override val elements = sortedMapOf<String, SpecTestsTypeStat>()
    override fun increment() {
        super.increment()
        section.increment()
    }
}

class SpecTestsSectionStat(private val area: SpecTestsStatElement) : SpecTestsStatElement() {
    override val elements = sortedMapOf<Int, SpecTestsParagraphStat>()
    override fun increment() {
        super.increment()
        area.increment()
    }
}

class SpecTestsAreaStat : SpecTestsStatElement() {
    override val elements = sortedMapOf<String, SpecTestsSectionStat>()
}

object TestsStatisticCollector {
    private const val TEST_DATA_DIR = "./testData"

    private fun incrementStatCounters(testAreaStats: SpecTestsAreaStat, sectionName: String, paragraphNumber: Int, testType: String) {
        val section = testAreaStats.elements.computeIfAbsent(sectionName) { SpecTestsSectionStat(testAreaStats) }
        val paragraph = section.elements.computeIfAbsent(paragraphNumber) { SpecTestsParagraphStat(section) }

        paragraph.elements.computeIfAbsent(testType) { SpecTestsTypeStat(paragraph) }.increment()
    }

    fun collect(): Map<TestArea, SpecTestsAreaStat> {
        val statistic = mutableMapOf<TestArea, SpecTestsAreaStat>()

        for (specTestArea in TestArea.values()) {
            val specTestsPath = "$TEST_DATA_DIR/${specTestArea.name.toLowerCase()}"

            statistic[specTestArea] = SpecTestsAreaStat()

            File(specTestsPath).walkTopDown().forEach areaTests@{
                if (!it.isFile || it.extension != "kt") return@areaTests

                val testInfoMatcher = SpecTestValidator.testPathPattern.matcher(it.path)

                if (!testInfoMatcher.find()) return@areaTests

                val sectionNumber = testInfoMatcher.group("sectionNumber")
                val sectionName = testInfoMatcher.group("sectionName")
                val paragraphNumber = testInfoMatcher.group("paragraphNumber").toInt()
                val testType = testInfoMatcher.group("testType")
                val section = "$sectionNumber $sectionName"

                incrementStatCounters(statistic[specTestArea]!!, section, paragraphNumber, testType)
            }
        }

        return statistic
    }
}