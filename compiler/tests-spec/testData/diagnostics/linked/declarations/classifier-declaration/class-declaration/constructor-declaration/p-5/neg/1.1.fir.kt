// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT


// TESTCASE NUMBER: 1
class Case1<T>() {
    class A(t: T)
    class B(x: List<T>)
    class C(c: () -> T)
    class E(n: Nothing, t: T)
}

// TESTCASE NUMBER: 2
class Case2<T>() {
    data class A(t: T)
    data class B(x: List<T>)
    data class C(c: () -> T)
    data class E(n: Nothing, t: T)
}

// TESTCASE NUMBER: 3
class Case3<T>() {
    enum class A(t: T)
    enum class B(x: List<T>)
    enum class C(c: () -> T)
    enum class E(n: Nothing, t: T)
}
