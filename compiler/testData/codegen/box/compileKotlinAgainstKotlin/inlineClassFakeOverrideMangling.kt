// !LANGUAGE: +InlineClasses

// MODULE: lib
// FILE: 1.kt

inline class IC(val s: String)

abstract class A {
    fun foo(s: String) = IC(s)
}

open class C : A()

class D: C()

// MODULE: main(lib)
// FILE: 2.kt

fun box(): String {
    var res = C().foo("OK").s
    if (res != "OK") return "FAIL 1 $res"
    res = D().foo("OK").s
    return res
}
