class Bar(val gav: String)

class Foo(val bar: Bar, val nbar: Bar?) {
    fun baz(s: String) = if (s != "") Bar(s) else null
}

fun String?.call(f: (String?) -> String?) = f(this)

fun String.notNullLet(f: (String) -> Unit) = f(this)

fun test(foo: Foo?) {
    foo?.bar?.gav.let {
        // Error, foo?.bar?.gav is nullable
        it<!UNSAFE_CALL!>.<!>length
        // Error, foo is nullable
        foo<!UNSAFE_CALL!>.<!>bar.gav.length
        // Correct
        foo?.bar?.gav?.length
    }
    foo?.bar?.gav.call { it }?.notNullLet {
        foo<!UNSAFE_CALL!>.<!>hashCode()
        foo<!UNSAFE_CALL!>.<!>bar.hashCode()
    }
}

fun testNotNull(foo: Foo) {
    val s: String? = ""
    foo.baz(s!!)?.gav.let {
        it<!UNSAFE_CALL!>.<!>length
        // Ok because of foo.
        <!DEBUG_INFO_SMARTCAST!>s<!>.length.hashCode()
    }
}

fun testNullable(foo: Foo?) {
    val s: String? = ""
    foo?.baz(s!!)?.gav.let {
        it<!UNSAFE_CALL!>.<!>length
        // Ok because of foo?.
        s?.length?.hashCode()
    }
}
