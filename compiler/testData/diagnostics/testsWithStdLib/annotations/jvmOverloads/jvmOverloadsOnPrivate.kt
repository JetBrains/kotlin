// !DIAGNOSTICS: -UNUSED_PARAMETER
class C {
    <!OVERLOADS_PRIVATE!>@kotlin.jvm.JvmOverloads<!> private fun foo(s: String = "OK") {
    }

    @kotlin.jvm.JvmOverloads fun bar(s: String = "OK") {
    }
}

fun foo() {
    <!OVERLOADS_LOCAL!>@kotlin.jvm.JvmOverloads<!> fun quux(s: String = "OK") {
    }

    class D {
        <!OVERLOADS_LOCAL!>@kotlin.jvm.JvmOverloads<!> fun foo(s: String = "OK") {
        }
    }

    val <!UNUSED_VARIABLE!>x<!> = object {
        <!OVERLOADS_LOCAL!>@kotlin.jvm.JvmOverloads<!> fun foo(s: String = "OK") {
        }
    }
}