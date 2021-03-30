// !DUMP_CFG
fun String.foo(b: Boolean): String = ""

fun String.let(block: () -> Unit) {}

fun test(x: String?) {
    x?.foo(x.length == 1)
    x<!UNSAFE_CALL!>.<!>length
}

interface A {
    fun bar(a: A): String
    fun bool(): Boolean
    fun id(): A
}

fun test_2(x: Any) {
    (x as? A)?.bar(x)
}

fun test_3(x: Any) {
    (x as? A)?.bar(x)?.foo(x.bool())?.let {
        x.bool()
    }
    x.<!UNRESOLVED_REFERENCE!>bool<!>()
}

fun test_4(x: A?) {
    x?.id()?.bool()
    x<!UNSAFE_CALL!>.<!>id()
}

fun Any?.boo(b: Boolean) {}

fun test_5(x: A?) {
    x?.let { return }?.boo(x.bool())
    x<!UNSAFE_CALL!>.<!>id()
}
