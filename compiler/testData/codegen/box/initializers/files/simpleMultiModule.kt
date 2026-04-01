// WITH_STDLIB

// MODULE: lib
// FILE: lib.kt
class X(val s: String)

val x = X("zzz")

// MODULE: lib2(lib)
// FILE: lib2.kt
class Z(val x: Int)

val z2 = Z(x.s.length)

// MODULE: main(lib2)
// FILE: main.kt
import kotlin.test.*

fun box(): String {
    assertEquals(3, z2.x)
    return "OK"
}
