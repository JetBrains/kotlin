// IGNORE_BACKEND_K1: ANY
// IGNORE_BACKEND_K2: ANY
// IGNORE_IR_DESERIALIZATION_TEST: NATIVE
// ^This code is not really supported and was allowed accidentally because of a bug in the corresponding checker.
// DISABLE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ This test passes frontend v2.1.0 and generated IR passes backend v2.2.0. However frontend v2.2.0 now raises diagnostic.
//     Backward test uses frontend v2.1.0 and backend v2.2.0, so backward test unexpectedly passes, and IGNORE directives make it red.
//     To avoid this, test is simply disabled for backward testing.

interface Foo<T> {
    fun foo(): T
}

interface Foo2 : Foo<String> {
    override fun foo(): String = "OK"
}

abstract class A1<T> : Foo<T>

open class A2 : A1<String>(), Foo2

open class A3 : A2() {
    fun test(): String = super.<!ABSTRACT_SUPER_CALL!>foo<!>()
}

class A4 : A3() {
    override fun foo(): String = "Fail"
}

fun box(): String = A4().test()
