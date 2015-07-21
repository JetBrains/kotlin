// !DIAGNOSTICS: -UNUSED_PARAMETER
class C {

    <!INAPPLICABLE_PUBLIC_FIELD!>@kotlin.jvm.publicField<!> constructor(s: String) {

    }

    <!INAPPLICABLE_PUBLIC_FIELD!>@kotlin.jvm.publicField<!> private fun foo(s: String = "OK") {

    }
}

<!INAPPLICABLE_PUBLIC_FIELD!>@kotlin.jvm.publicField<!>
fun foo() {
    <!INAPPLICABLE_PUBLIC_FIELD!>@kotlin.jvm.publicField<!> val <!UNUSED_VARIABLE!>x<!> = "A"
}