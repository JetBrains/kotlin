// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-74107
// LANGUAGE: +NestedTypeAliases
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ Compiler v2.1.10 does not know this language feature

// FILE: staticScope.kt

class Bar {
    inner class Inner {
        val p: String
            get() = "OK"
    }
    inner class Inner2<T>(val p: T)

    class Nested<T>(val p: T)

    typealias TAtoInner = Inner
    typealias TA2toInner2<K> = Inner2<K>
    typealias TAtoNested = Nested<String>

    fun bar(): String {
        if (TAtoInner().p != "OK") return "FAIL"
        if (TA2toInner2("OK").p != "OK") return "FAIL"
        if (TAtoNested("OK").p != "OK") return "FAIL"

        val callable = ::TAtoInner
        if (callable().p != "OK") return "FAIL"

        return "OK"
    }
}

// FILE: main.kt

import Bar.TAtoInner
import Bar.TA2toInner2

fun test(): String {
    val bar = Bar()

    if (bar.TAtoInner().p != "OK") return "FAIL"
    if (bar.TA2toInner2("OK").p != "OK") return "FAIL"

    return "OK"
}

fun box(): String {
    if (test() != "OK") return "FAIL"

    return Bar().bar()
}