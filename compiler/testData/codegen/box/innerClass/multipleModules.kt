// MODULE: lib
// FILE: lib.kt

open class A {
    open inner class Inner {
        val x = "OK"
    }
}

// MODULE: main(lib)
// FILE: main.kt

open class B : A() {
    open inner class Inner : A.Inner()
}

fun box() = B().Inner().x