/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.benchmark.createdFiles

import org.jetbrains.kotlin.utils.Printer
import kotlin.random.Random

class TestCasesGenerator {
    private val charPool: List<Char> = ('a'..'z') + ('A'..'Z')
    private val types = listOf("Int", "Double", "Boolean", "String")

    private val builder = StringBuilder()
    private val printer: Printer

    init {
        printer = Printer(builder, "    ")
    }

    fun generateData(vararg pipeline: TestCasesGenerator.() -> Unit): TestCasesGenerator {
        printer.apply {
            pipeline.forEach { it() }
        }
        return this
    }

    fun createClass(count: Int, vararg pipeline: TestCasesGenerator.() -> Unit) {
        for (i in 1..count) {
            printer.println("class ${randomString(Random.nextLong(5, 15))} {")
            printer.pushIndent()
            printer.apply {
                pipeline.forEach { it() }
            }
            printer.popIndent()
            printer.println("}")
        }
    }

    fun createFun(count: Int, vararg pipeline: TestCasesGenerator.() -> Unit) {
        for (i in 1..count) {
            printer.println("fun ${randomString(Random.nextLong(5, 15))}() {")
            printer.pushIndent()
            printer.apply {
                pipeline.forEach { it() }
            }
            printer.popIndent()
            printer.println("}")
        }
    }

    fun createProperty(count: Int) {
        for (i in 1..count) {
            val typeAndValue = getRandomTypeAndValue()
            printer.println(
                "val ${randomString(Random.nextLong(5, 15))}: " +
                        "${typeAndValue.first} = ${typeAndValue.second} \n"
            )
        }
    }

    private fun randomString(length: Long): String {
        return (1..length)
            .map { Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    private fun getRandomTypeAndValue(): Pair<String, String> {
        val type = types[Random.nextInt(0, types.size)]
        return when (type) {
            "Int" -> Pair(type, Random.nextInt().toString())
            "Double" -> Pair(type, Random.nextDouble().toString())
            "Boolean" -> Pair(type, Random.nextBoolean().toString())
            "String" -> Pair(type, randomString(Random.nextLong(5, 15)))
            else -> throw UnsupportedOperationException("Unsupported type")
        }
    }

    fun getText(): String {
        return builder.toString()
    }
}

fun main() {
    TestCasesGenerator().generateData(
        {
            createClass(10,
                        {
                            createClass(100,
                                        {
                                            createClass(100)
                                        })
                        })
        }
    )
}

