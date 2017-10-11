package runtime.text.string0

import kotlin.test.*

@Test fun runTest() {
    val str = "hello"
    println(str.equals("HElLo", true))
    val strI18n = "Привет"
    println(strI18n.equals("прИВет", true))
    println(strI18n.toUpperCase())
    println(strI18n.toLowerCase())
    println("пока".capitalize())
    println("http://jetbrains.com".startsWith("http://"))
}