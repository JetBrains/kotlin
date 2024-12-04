// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.test.assertEquals

class Outer<O> {
    class Nested

    inner class Inner
}

fun outer(): Outer<String> = null!!
fun nested(): Outer.Nested = null!!
fun inner(): Outer<Int>.Inner = null!!

fun array(): Array<String> = null!!

fun box(): String {
    assertEquals(Outer::class, ::outer.returnType.classifier)
    assertEquals(Outer.Nested::class, ::nested.returnType.classifier)
    assertEquals(Outer.Inner::class, ::inner.returnType.classifier)

    assertEquals(Array<String>::class, ::array.returnType.classifier)

    return "OK"
}
