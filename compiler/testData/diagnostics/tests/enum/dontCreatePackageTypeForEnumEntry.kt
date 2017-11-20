enum class E {
    FIRST,
    SECOND {
        <!NESTED_CLASS_DEPRECATED!>class A<!>
    };
}

val foo: Any.() -> Unit = {}

fun f1() = E.FIRST.foo()
fun f2() = E.FIRST.(foo)()
fun f3() = E.SECOND.foo()
fun f4() = E.SECOND.(foo)()
fun f5() = E.SECOND.<!UNRESOLVED_REFERENCE!>A<!>()
