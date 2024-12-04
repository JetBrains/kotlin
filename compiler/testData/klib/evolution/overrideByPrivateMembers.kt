// ISSUE: KT-64972

// MODULE: lib1
// FILE: 1.kt
// VERSION: 1

open class A {
    fun test() = "Fail"
}

// FILE: 2.kt
// VERSION: 2

open class A {
    open fun foo() = "OK"
    fun test() = foo()
}

// MODULE: lib2(lib1)
// FILE: lib2.kt
class B : A() {
    private fun foo() = "Fail"
}

// MODULE: mainLib(lib2, lib1)
// FILE: mainLib.kt

fun lib2() = B().test()

// MODULE: main(mainLib)
// FILE: main.kt

fun box(): String = lib2()
