/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec

import org.jetbrains.kotlin.spec.validators.*
import java.io.File

open class SpecTestsStatElement(val type: SpecTestsStatElementType) {
    val elements: MutableMap<Any, SpecTestsStatElement> = mutableMapOf()
    var number = 0
    fun increment() {
        number++
    }
}

enum class SpecTestsStatElementType {
    TYPE,
    CATEGORY,
    PARAGRAPH,
    SECTION,
    AREA
}

object TestsStatisticCollector {
    private const val TEST_DATA_DIR = "./testData"

    private fun incrementStatCounters(baseStatElement: SpecTestsStatElement, elementTypes: List<Pair<SpecTestsStatElementType, Any>>) {
        var currentStatElement = baseStatElement

        baseStatElement.increment()

        for ((elementType, value) in elementTypes) {
            currentStatElement = currentStatElement.run {
                elements.computeIfAbsent(value) { SpecTestsStatElement(elementType) }.apply { increment() }
            }
        }
    }

    fun collect(testLinkedType: SpecTestLinkedType): Map<TestArea, SpecTestsStatElement> {
        val statistic = mutableMapOf<TestArea, SpecTestsStatElement>()

        for (specTestArea in TestArea.values()) {
            val specTestsPath =
                "$TEST_DATA_DIR/${specTestArea.name.toLowerCase()}/${AbstractSpecTestValidator.dirsByLinkedType[testLinkedType]}"

            statistic[specTestArea] = SpecTestsStatElement(SpecTestsStatElementType.AREA)

            File(specTestsPath).walkTopDown().forEach areaTests@{
                if (!it.isFile || it.extension != "kt") return@areaTests

                val specTestsValidator = AbstractSpecTestValidator.getInstanceByType(it)

                try {
                    specTestsValidator.parseTestInfo()
                } catch (e: SpecTestValidationException) {
                    return@areaTests
                }

                incrementStatCounters(
                    statistic[specTestArea]!!,
                    when (testLinkedType) {
                        SpecTestLinkedType.LINKED -> getStatElementsByLinkedTests(specTestsValidator.testInfo as LinkedSpecTest)
                        SpecTestLinkedType.NOT_LINKED -> getStatElementsByNotLinkedTests(specTestsValidator.testInfo as NotLinkedSpecTest)
                    }
                )
            }
        }

        return statistic
    }

    private fun getStatElementsByLinkedTests(testInfo: LinkedSpecTest) = listOf(
        SpecTestsStatElementType.SECTION to testInfo.section,
        SpecTestsStatElementType.PARAGRAPH to testInfo.paragraphNumber,
        SpecTestsStatElementType.TYPE to testInfo.testType.type
    )

    private fun getStatElementsByNotLinkedTests(testInfo: NotLinkedSpecTest) =
        mutableListOf(SpecTestsStatElementType.SECTION to testInfo.section).apply {
            addAll(testInfo.categories.map { SpecTestsStatElementType.CATEGORY to it })
            add(SpecTestsStatElementType.TYPE to testInfo.testType.type)
        }
}