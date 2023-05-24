// FIR_IDENTICAL
<!UNSUPPORTED_FEATURE!>expect<!> fun foo1()
<!UNSUPPORTED_FEATURE!>expect<!> val bar1 = <!EXPECTED_PROPERTY_INITIALIZER!>42<!>
<!UNSUPPORTED_FEATURE!>expect<!> class Baz1 {
    fun foo()

    class Baz12
}

<!UNSUPPORTED_FEATURE!>actual<!> fun foo2() = 42
<!MUST_BE_INITIALIZED!><!UNSUPPORTED_FEATURE!>actual<!> val bar2: Int<!>
<!UNSUPPORTED_FEATURE!>actual<!> interface Baz2
