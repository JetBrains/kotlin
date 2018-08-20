//ALLOW_AST_ACCESS
package test

class A {
    fun String.f1() {
    }

    fun f1() {
    }

    fun Int.f1() {
    }

    val Int.c: Int
        get() = 1

    val c: Int = { 2 }()

    val d: Int = { 2 }()

    val Int.d: Int
        get() = 1

    fun String.f2() {
    }

    fun f2() {
    }

    fun Int.f2() {
    }

    fun String.f3() {
    }

    fun f3() {
    }

    fun Int.f3() {
    }
}