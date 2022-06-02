/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList

class BuildInputs : Serializable {

    private val myInputs = TreeMap<String, MutableList<String>>()

    fun add(propertyName: String, value: Any?) {
        when (value) {
            is Map<*, *> -> {
                value.forEach {
                    myInputs.getOrPut("$propertyName.${it.key}") { ArrayList() }.add(it.value.toString())
                }
            }
            is Iterable<*> -> {
                myInputs.getOrPut(propertyName) { ArrayList() }.addAll(value.map { it.toString() })
            }
            else -> {
                myInputs.getOrPut(propertyName) { ArrayList() }.add(value.toString())
            }
        }
    }

    fun addAll(other: BuildInputs) {
        for ((property, value) in other.myInputs) {
            add(property, value)
        }
    }

    fun asMap(): Map<String, List<String>> = myInputs

    companion object {
        const val serialVersionUID = 0L
        const val fileProperty = "FILE"
    }
}