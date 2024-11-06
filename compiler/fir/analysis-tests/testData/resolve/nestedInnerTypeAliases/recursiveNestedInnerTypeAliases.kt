// LANGUAGE: +NestedTypeAliases

class C {
    typealias TA = C
}

class C1 {
    inner class Inner

    typealias TA1 = C2 // OK

    typealias TA2 = <!RECURSIVE_TYPEALIAS_EXPANSION!>TA3<!>
    typealias TA3 = <!RECURSIVE_TYPEALIAS_EXPANSION!>TA2<!>

    typealias Nested = Inner
}

class C2 {
    typealias TA2 = C1
}

class D1<T> {
    inner typealias TA1 = D2<T> // OK

    inner typealias TA2 = <!RECURSIVE_TYPEALIAS_EXPANSION!>TA3<!>
    inner typealias TA3 = <!RECURSIVE_TYPEALIAS_EXPANSION!>TA2<!>
}

class D2<T> {
    inner typealias TA2 = D1<T>
}

class E : <!CYCLIC_INHERITANCE_HIERARCHY!>E.TA<!> {
    typealias TA = <!RECURSIVE_TYPEALIAS_EXPANSION!>E<!>
}

fun test() {
    C1.TA1()
    D1<String>().<!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>TA1<!>()
}
