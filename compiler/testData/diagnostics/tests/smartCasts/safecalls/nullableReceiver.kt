// See KT-10056
class Foo(val bar: String)

public inline fun <T, R> T.let(f: (T) -> R): R = f(this)

fun test(foo: Foo?) {
    foo?.bar.let {
        // Error, foo?.bar is nullable
        it<!UNSAFE_CALL!>.<!>length
        // Error, foo is nullable
        foo<!UNSAFE_CALL!>.<!>bar.length
        // Correct
        foo?.bar?.length
    }
}