package datagen.literals.strdedup2

import kotlin.test.*

@Test fun runTest() {
    val str1 = ""
    val str2 = "hello".subSequence(2, 2)
    println(str1 == str2)
    println(str1 === str2)
}
