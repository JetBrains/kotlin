// !DIAGNOSTICS: -UNUSED_VARIABLE

fun foo() {
    <!LOCAL_ENUM_NOT_ALLOWED!>enum class A<!> {
        FOO,
        BAR
    }
    val foo = A.FOO
    val b = object {
        <!LOCAL_ENUM_NOT_ALLOWED!>enum class B<!> {}
    }
    class C {
        <!LOCAL_ENUM_NOT_ALLOWED!>enum class D<!> {}
    }
    val f = {
        <!LOCAL_ENUM_NOT_ALLOWED!>enum class E<!> {}
    }

    enum class<!SYNTAX!><!> {}
}
