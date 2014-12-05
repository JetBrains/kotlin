// !MARK_DYNAMIC_CALLS

fun foo(d: dynamic) {
    Foo(d).p.<!DEBUG_INFO_DYNAMIC!>bar<!>()

}

class Foo<T>(val p: T)