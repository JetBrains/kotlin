// !DIAGNOSTICS: -UNUSED_PARAMETER

class Inv<T>

fun <T> foo(f: () -> T): Inv<T> = TODO()

fun myExit(): Nothing = TODO()

fun test(x: String?): Inv<String> {
    return foo {
        if (x == null) myExit()
        <!DEBUG_INFO_SMARTCAST!>x<!>
    }
}
