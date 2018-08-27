/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.delegatedProperty.simpleVar

import kotlin.test.*

import kotlin.reflect.KProperty

class Delegate {
    var f: Int = 42

    operator fun getValue(receiver: Any?, p: KProperty<*>): Int {
        println("get ${p.name}")
        return f
    }

    operator fun setValue(receiver: Any?, p: KProperty<*>, value: Int) {
        println("set ${p.name}")
        f = value
    }
}

class C {
    var x: Int by Delegate()
}

@Test fun runTest() {
    val c = C()
    println(c.x)
    c.x = 117
    println(c.x)
}