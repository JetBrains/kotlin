// MODULE: lib
// FILE: lib.kt

open class Outer(val x: String) {
    open inner class Inner1
    inner class Middle(x: String) : Outer(x) {
        inner class Inner2 : Inner1() {
            fun foo() = this@Outer.x + this@Middle.x
        }
    }
}

// MODULE: main(lib)
// FILE: main.kt

fun box() = Outer("O").Middle("K").Inner2().foo()