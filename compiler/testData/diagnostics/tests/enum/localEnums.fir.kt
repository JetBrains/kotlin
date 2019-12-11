// !DIAGNOSTICS: -UNUSED_VARIABLE

fun foo() {
    enum class A {
        FOO,
        BAR
    }
    val foo = A.<!UNRESOLVED_REFERENCE!>FOO<!>
    val b = object {
        enum class B {}
    }
    class C {
        enum class D {}
    }
    val f = {
        enum class E {}
    }

    enum class<!SYNTAX!><!> {}
}
