// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT


// TESTCASE NUMBER: 1
data class A(<!DATA_CLASS_NOT_PROPERTY_PARAMETER!>x: Any?<!>)
data class B(<!DATA_CLASS_NOT_PROPERTY_PARAMETER!>x: Any<!>)
data class C(<!DATA_CLASS_NOT_PROPERTY_PARAMETER!>c: () -> Any<!>)
data class D(<!DATA_CLASS_NOT_PROPERTY_PARAMETER!>e: Enum<*><!>)
data class E(<!DATA_CLASS_NOT_PROPERTY_PARAMETER!>n: Nothing<!>)
data class F<T>(<!DATA_CLASS_NOT_PROPERTY_PARAMETER!>t: T<!>)


// TESTCASE NUMBER: 2
class Case2<T>() {
    data class A(<!DATA_CLASS_NOT_PROPERTY_PARAMETER!>t: T<!>)
    data class B(<!DATA_CLASS_NOT_PROPERTY_PARAMETER!>x: List<T><!>)
    data class C(<!DATA_CLASS_NOT_PROPERTY_PARAMETER!>c: () -> T<!>)
    data class E(<!DATA_CLASS_NOT_PROPERTY_PARAMETER!>n: Nothing<!>, <!DATA_CLASS_NOT_PROPERTY_PARAMETER!>t: T<!>)
}
