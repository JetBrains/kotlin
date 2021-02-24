// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT


// TESTCASE NUMBER: 1
class Case1<T>() {
    class A(var t: T)
    class B(var x: List<T>)
    class C(var c: () -> T)
    class E(var n: Nothing, var t: T)
}
