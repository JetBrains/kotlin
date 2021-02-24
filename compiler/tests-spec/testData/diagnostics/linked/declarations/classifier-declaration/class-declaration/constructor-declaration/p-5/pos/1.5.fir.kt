// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// TESTCASE NUMBER: 1
fun <T> List<T>.case1() {
    class Case1(t: T)
    class A(t: T)
    class B(x: List<T>)
    class C(c: () -> T)
    class E(n: Nothing, t: T)
}

// TESTCASE NUMBER: 2
val <T> List<T>.case2: Int
    get() = {
        class A(t: T)
        class B(x: List<T>)
        class C(c: () -> T)
        class E(n: Nothing, t: T)
        1
    }()

// TESTCASE NUMBER: 3
var <T> List<T>.case3: Unit
    get() {
        class A(t: T)
        class B(x: List<T>)
        class C(c: () -> T)
        class E(n: Nothing, t: T)
        1
    }
    set(i: Unit) {
        class A(t: T)
        class B(x: List<T>)
        class C(c: () -> T)
        class E(n: Nothing, t: T)
    }
