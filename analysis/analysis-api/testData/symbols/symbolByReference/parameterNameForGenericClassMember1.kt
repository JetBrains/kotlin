// DO_NOT_CHECK_SYMBOL_RESTORE
class Foo<T> {
    fun foo(param: Int) {}
}

fun Foo<Int>.usage() {
    foo(pa<caret>ram = 10)
}

