/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.tasks

import org.jetbrains.kotlin.spec.SpecTestLinkedType
import org.jetbrains.kotlin.spec.utils.SpecTestsStatElement
import org.jetbrains.kotlin.spec.utils.SpecTestsStatElementType
import org.jetbrains.kotlin.spec.utils.TestsStatisticCollector

const val PRINT_BASE_INDENT = "  "

fun linkedSpecTestsPrint() {
    println("LINKED SPEC TESTS STATISTIC")
    println("--------------------------------------------------")

    val statistic = TestsStatisticCollector.collect(SpecTestLinkedType.LINKED)

    for ((areaName, areaElement) in statistic) {
        println("$areaName: ${areaElement.number} tests")
        for ((sectionName, sectionElement) in areaElement.elements) {
            print("  $sectionName: ${sectionElement.number} tests")
            notLinkedSpecTestsCategoriesPrint(sectionElement.elements)
            println()
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