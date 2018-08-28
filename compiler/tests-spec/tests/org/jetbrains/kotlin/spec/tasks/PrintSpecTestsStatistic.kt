/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.tasks

import org.jetbrains.kotlin.spec.SpecTestsStatElement
import org.jetbrains.kotlin.spec.SpecTestsStatElementType
import org.jetbrains.kotlin.spec.TestsStatisticCollector
import org.jetbrains.kotlin.spec.validators.SpecTestLinkedType

const val PRINT_BASE_INDENT = "  "

fun linkedSpecTestsPrint() {
    println("SPEC TESTS STATISTIC")
    println("--------------------------------------------------")

    val statistic = TestsStatisticCollector.collect(SpecTestLinkedType.LINKED)

    for ((areaName, areaElement) in statistic) {
        println("$areaName: ${areaElement.number} tests")
        for ((sectionName, sectionElement) in areaElement.elements) {
            println("  $sectionName: ${sectionElement.number} tests")
            for ((paragraphName, paragraphElement) in sectionElement.elements) {
                val testsStatByType = mutableListOf<String>()
                for ((typeName, typeElement) in paragraphElement.elements)
                    testsStatByType.add(" [ $typeName: ${typeElement.number} ]")
                print(PRINT_BASE_INDENT.repeat(2))
                println("PARAGRAPH $paragraphName: ${paragraphElement.number} tests${testsStatByType.joinToString("")}")
            }
        }
    }
}

fun notLinkedSpecTestsCategoriesPrint(elements: Map<Any, SpecTestsStatElement>, level: Int = 1) {
    for ((name, element) in elements) {
        if (element.type == SpecTestsStatElementType.TYPE) {
            print(" [ $name: ${element.number} ]")
            continue
        }

        println()
        print("${PRINT_BASE_INDENT.repeat(level)}$name: ${element.number} tests")

        notLinkedSpecTestsCategoriesPrint(element.elements, level + 1)
    }
}

fun notLinkedSpecTestsPrint() {
    println("NOT LINKED SPEC TESTS STATISTIC")
    println("--------------------------------------------------")

    val statistic = TestsStatisticCollector.collect(SpecTestLinkedType.NOT_LINKED)

    for ((areaName, areaElement) in statistic) {
        println("$areaName: ${areaElement.number} tests")
        for ((sectionName, sectionElement) in areaElement.elements) {
            print("  $sectionName: ${sectionElement.number} tests")
            notLinkedSpecTestsCategoriesPrint(sectionElement.elements)
            println()
        }
    }
}

fun main(args: Array<String>) {
    println("==================================================")
    linkedSpecTestsPrint()
    println("==================================================")
    notLinkedSpecTestsPrint()
    println("==================================================")
}