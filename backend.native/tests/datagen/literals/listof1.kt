package datagen.literals.listof1

import kotlin.test.*

@Test fun runTest() {
    val list = foo()
    println(list === foo())
    println(list.toString())
}

fun foo(): List<String> {
    return listOf("a", "b", "c")
}