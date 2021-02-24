// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT


// TESTCASE NUMBER: 1
data class A(x: Any?)
data class B(x: Any)
data class C(c: () -> Any)
data class D(e: Enum<*>)
data class E(n: Nothing)
data class F<T>(t: T)


// TESTCASE NUMBER: 2
class Case2<T>() {
    data class A(t: T)
    data class B(x: List<T>)
    data class C(c: () -> T)
    data class E(n: Nothing, t: T)
}
