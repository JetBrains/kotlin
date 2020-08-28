// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT


// TESTCASE NUMBER: 1
class Case1 {
    class A(var x: Any?)
    class B(var x: Any)
    class C(var c: () -> Any)
    class D(var e: Enum<*>)
    class E(var n: Nothing)
    class F<T>(var t: T)
}

// TESTCASE NUMBER: 2
class Case2<T>() {
    class A<T : CharSequence>(var e: T)
    class B<T : java.util.AbstractCollection<Int>>(var e: T)
    class C<T : java.lang.Exception>(var e: T)
    class D<T : Enum<*>>(var e: T)
    class F<T>(var t: T)
}
