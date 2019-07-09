// NI_EXPECTED_FILE

package i

val <T> List<T>.length = <!EXTENSION_PROPERTY_WITH_BACKING_FIELD, UNRESOLVED_REFERENCE!>size<!>

val <T> List<T>.length1 : Int get() = size

val String.bd = <!EXTENSION_PROPERTY_WITH_BACKING_FIELD!><!NO_THIS!>this<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>+<!> "!"<!>

val String.bd1 : String get() = this + "!"


class A {
    val ii : Int = 1
}

val A.foo = <!EXTENSION_PROPERTY_WITH_BACKING_FIELD, UNRESOLVED_REFERENCE!>ii<!>

val A.foo1 : Int get() = ii


class C {
    inner class D {}
}

val C.foo : C.D = <!EXTENSION_PROPERTY_WITH_BACKING_FIELD!><!UNRESOLVED_REFERENCE!>D<!>()<!>

val C.bar : C.D = <!EXTENSION_PROPERTY_WITH_BACKING_FIELD!>C().D()<!>

val C.foo1 : C.D get() = D()
