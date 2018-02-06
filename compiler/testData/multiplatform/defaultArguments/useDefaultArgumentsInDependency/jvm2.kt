package usage

import lib.*

fun testFunction() {
    foo(42)
    foo(42, "OK")
}

fun testConstructor() {
    C(42)
    C(42, "OK")
}

@Anno1(42)
fun testAnno1a() {}

@Anno1(42, "OK")
fun testAnno1b() {}

@Anno2(42)
fun testAnno2a() {}

@Anno2(42, "OK")
fun testAnno2b() {}
