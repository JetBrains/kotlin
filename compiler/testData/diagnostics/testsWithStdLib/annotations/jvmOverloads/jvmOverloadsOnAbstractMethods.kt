interface T {
    <!OVERLOADS_ABSTRACT!>@kotlin.jvm.JvmOverloads<!> fun foo(s: String = "OK")
}


abstract class C {
    <!OVERLOADS_ABSTRACT!>@kotlin.jvm.JvmOverloads<!> abstract fun foo(s: String = "OK")
}