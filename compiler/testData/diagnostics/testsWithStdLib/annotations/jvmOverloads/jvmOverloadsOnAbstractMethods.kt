interface T {
    <!OVERLOADS_ABSTRACT!>@kotlin.jvm.jvmOverloads fun foo(s: String = "OK")<!>
}


abstract class C {
    <!OVERLOADS_ABSTRACT!>@kotlin.jvm.jvmOverloads abstract fun foo(s: String = "OK")<!>
}