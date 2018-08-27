/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.basic.entry0

fun fail() {
    println("Test failed, this is a wrong main() function.")
}

fun main() {
    fail()
}

fun <T> main(args: Array<String>) {
    fail()
}

fun main(args: Array<Int>) {
    fail()
}

fun main(args: Array<String>, second_arg: Int) {
    fail()
}

class Foo {
    fun main(args: Array<String>) {
        fail()
    }
}

fun main(args: Array<String>) {
    println("Hello.")
}

