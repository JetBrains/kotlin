// ISSUE: KT-64261
// DUMP_IR
interface I<T : Any> {
    fun foo(func: () -> T?)
}

class Impl<T : Any> : I<T> {
    override fun foo(func: () -> T?) {
        func()
    }
}

fun I<*>.foo() {
    foo { null }
}

fun box(): String {
    val foo = Impl<String>()
    foo.foo()
    return "OK"
}
