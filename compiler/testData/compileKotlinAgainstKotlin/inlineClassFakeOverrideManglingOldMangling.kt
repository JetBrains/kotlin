// TARGET_BACKEND: JVM
// !LANGUAGE: +InlineClasses
// FILE: 1.kt
// KOTLIN_CONFIGURATION_FLAGS: +JVM.USE_OLD_INLINE_CLASSES_MANGLING_SCHEME

inline class IC(val s: String)

abstract class A {
    fun foo(s: String) = IC(s)
}

open class C : A()

class D: C()

// FILE: 2.kt

fun box(): String {
    var res = C().foo("OK").s
    if (res != "OK") return "FAIL 1 $res"
    res = D().foo("OK").s
    return res
}
