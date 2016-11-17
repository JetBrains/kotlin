// !DIAGNOSTICS: -UNUSED_PARAMETER
class C {
    <!OVERLOADS_PRIVATE!>@kotlin.jvm.JvmOverloads<!> private fun foo(s: String = "OK") {
    }

    @kotlin.jvm.JvmOverloads fun bar(s: String = "OK") {
    }
}

fun foo() {
    class D {
        <!OVERLOADS_PRIVATE!>@kotlin.jvm.JvmOverloads<!> fun foo(s: String = "OK") {
        }
    }

    val <!UNUSED_VARIABLE!>x<!> = object {
        <!OVERLOADS_PRIVATE!>@kotlin.jvm.JvmOverloads<!> fun foo(s: String = "OK") {
    }
    }
}