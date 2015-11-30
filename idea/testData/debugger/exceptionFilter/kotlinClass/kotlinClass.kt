class A {
    fun foo() {
        null!!
    }
}

fun box() {
    A().foo()
}

// FILE: kotlinClass.kt
// LINE: 3