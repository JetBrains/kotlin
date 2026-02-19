// MODULE: lib
// FILE: lib.kt

class A {
    val x: String

    init {
        var y = "FAIL"

        fun foo() { y = "OK" }

        foo()

        x = y
    }
}

// MODULE: main(lib)
// FILE: main.kt

fun box() = A().x