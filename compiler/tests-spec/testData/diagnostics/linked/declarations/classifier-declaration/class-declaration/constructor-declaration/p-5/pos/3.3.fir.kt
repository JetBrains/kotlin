// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT


// TESTCASE NUMBER: 1
class Case1 {
    inner class A(var x: Any?)
    inner class B(var x: Any)
    inner class C(var c: () -> Any)
    inner class D(var e: Enum<*>)
    inner class E(var n: Nothing)
    inner class F<T>(var t: T)
}

// TESTCASE NUMBER: 2
class Case2<T>() {
    inner class A(var x: Any?, t: T)
    inner class B(var x: T)
    inner class C(var c: () -> T)
    inner class D<T : Enum<*>>(var e: T)
    inner class E(var n: Nothing, var t: T)
    inner class F<T>(var t: T)
}
