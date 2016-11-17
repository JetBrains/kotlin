interface T {
    <!OVERLOADS_INTERFACE!>@kotlin.jvm.JvmOverloads<!> fun foo(s: String = "OK")

    <!OVERLOADS_INTERFACE!>@kotlin.jvm.JvmOverloads<!> fun bar(s: String = "OK") {
    }
}


abstract class C {
    <!OVERLOADS_ABSTRACT!>@kotlin.jvm.JvmOverloads<!> abstract fun foo(s: String = "OK")
}