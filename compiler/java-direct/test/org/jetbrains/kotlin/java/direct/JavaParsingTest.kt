/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.junit.Test

class JavaParsingTest {

    @Test
    fun testBasicJavaParsing() {
        val source = "class A {}"
        val builder = parseJavaToSyntaxTreeBuilder(source, 0)
        val root = buildDirectSyntaxTree(builder, source)
        val javaClass = root.children.first { it.type.toString() == "CLASS" }.let { JavaClassDirectImpl(it, source) }
        assert(javaClass.name.asString() == "A")
    }
}
