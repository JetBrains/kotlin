/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

fun measureElementDistribution(birTree: BirElement) {
    class Metric(val className: String) {
        var total = 0
    }

    val elementsByClass = mutableMapOf<Class<*>, Metric>()
    birTree.accept { element ->
        val cls = element.javaClass
        val metric: Metric = elementsByClass.computeIfAbsent(cls) { Metric(cls.simpleName.removeSuffix("Impl")) }
        metric.total++
        element.walkIntoChildren()
    }
    println(elementsByClass.values.joinToString("\n") { "${it.className} ${it.total}" })
}