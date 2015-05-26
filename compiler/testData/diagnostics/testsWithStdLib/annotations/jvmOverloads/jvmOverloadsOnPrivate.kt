// !DIAGNOSTICS: -UNUSED_PARAMETER
class C {
    <!OVERLOADS_PRIVATE!>@kotlin.jvm.jvmOverloads private fun foo(s: String = "OK")<!> {
    }

    @kotlin.jvm.jvmOverloads fun bar(s: String = "OK") {
    }
}

fun foo() {
    class D {
        <!OVERLOADS_PRIVATE!>@kotlin.jvm.jvmOverloads fun foo(s: String = "OK")<!> {
        }
    }

    val <!UNUSED_VARIABLE!>x<!> = object {
        <!OVERLOADS_PRIVATE!>@kotlin.jvm.jvmOverloads fun foo(s: String = "OK")<!> {
    }
    }
}