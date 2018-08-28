/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.tasks

import org.jetbrains.kotlin.spec.TestsStatisticCollector

fun main(args: Array<String>) {
    val statistic = TestsStatisticCollector.collect()

    println("--------------------------------------------------")
    println("SPEC TESTS STATISTIC")
    println("--------------------------------------------------")

    for ((areaName, areaElement) in statistic) {
        println("$areaName: ${areaElement.counter} tests")

        for ((sectionName, sectionElement) in areaElement.elements) {
            println("  ${sectionName.toUpperCase()}: ${sectionElement.counter} tests")

            for ((paragraphName, paragraphElement) in sectionElement.elements) {
                val testsStatByType = mutableListOf<String>()

                for ((typeName, typeElement) in paragraphElement.elements) {
                    testsStatByType.add("$typeName: ${typeElement.counter}")
                }

                println("    PARAGRAPH $paragraphName: ${paragraphElement.counter} tests (${testsStatByType.joinToString(", ")})")
            }
        }
    }

    println("--------------------------------------------------")
}