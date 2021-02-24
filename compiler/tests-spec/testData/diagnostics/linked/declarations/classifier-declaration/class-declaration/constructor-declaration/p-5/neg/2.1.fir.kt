// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT


// TESTCASE NUMBER: 1
class Case1<T>() {
    class A(val t: T)
    class B(val x: List<T>)
    class C(val c: () -> T)
    class E(val n: Nothing, val t: T)
}
