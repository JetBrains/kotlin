// MODULE: lib
// FILE: lib.kt

class C {
    val c: C? = null
}

// MODULE: main(lib)
// FILE: main.kt

fun main() {
    val local: C? = C()
    println(<expr>local?.c</expr> != null)
}
