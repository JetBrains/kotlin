package runtime.basic.hello4

import kotlin.test.*

@Test fun runTest() {
    val x = 2
    println(if (x == 2) "Hello" else "Привет")
    println(if (x == 3) "Bye" else "Пока")
 }