package datagen.literals.strdedup1

import kotlin.test.*

@Test fun runTest() {
    val str1 = "Hello"
    val str2 = "Hello"
    println(str1 == str2)
    println(str1 === str2)
}
