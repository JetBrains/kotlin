// !DIAGNOSTICS: -UNUSED_PARAMETER
class C {
    <!OVERLOADS_PRIVATE!>[kotlin.jvm.overloads] private fun foo(s: String = "OK")<!> {
    }

    [kotlin.jvm.overloads] fun bar(s: String = "OK") {
    }
}

fun foo() {
    class D {
        <!OVERLOADS_PRIVATE!>[kotlin.jvm.overloads] fun foo(s: String = "OK")<!> {
        }
    }

    val <!UNUSED_VARIABLE!>x<!> = object {
        <!OVERLOADS_PRIVATE!>[kotlin.jvm.overloads] fun foo(s: String = "OK")<!> {
    }
    }
}