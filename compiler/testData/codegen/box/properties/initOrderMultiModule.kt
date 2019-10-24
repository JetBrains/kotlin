// MODULE: lib
// FILE: lib.kt

// KT-34273

class Foo(val str: String)

private val foo1 = Foo("OK")

val foo2 = foo1

// MODULE: main(lib)
// FILE: main.kt

fun box(): String = foo2.str
