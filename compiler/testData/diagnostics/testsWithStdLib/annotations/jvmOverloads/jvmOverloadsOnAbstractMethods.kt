trait T {
    <!OVERLOADS_ABSTRACT!>[kotlin.jvm.overloads] fun foo(s: String = "OK")<!>
}


abstract class C {
    <!OVERLOADS_ABSTRACT!>[kotlin.jvm.overloads] abstract fun foo(s: String = "OK")<!>
}