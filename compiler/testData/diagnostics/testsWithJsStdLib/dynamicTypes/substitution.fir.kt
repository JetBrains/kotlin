// !MARK_DYNAMIC_CALLS

fun foo(d: dynamic) {
    Foo(d).p.bar()

}

class Foo<T>(val p: T)
