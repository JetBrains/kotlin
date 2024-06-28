// ISSUE: KT-62863

fun <K> materialize(): K = null!!

interface Foo<out T> {
    fun foo()
}

abstract class Bar<T : Any> {
    abstract fun bar()
}

fun <T> Foo<T>.extFoo() {}
fun <T : Any> Bar<T>.extBar() {}

fun test_1(x: Any) {
    x as Bar<Any>
    x as Foo<Any>

    x.foo()
    x.bar()
    x.extFoo<Any>()
    x.extBar<Any>()
}

fun test_2(x: Any) {
    x as Bar<Any>?
    x as Foo<Any>?

    x!!.foo()
    x!!.bar()
    x!!.extFoo<Any>()
    x!!.extBar<Any>()
}

fun test_3(x: Any) {
    x as Bar<Any>?
    x as Foo<Any>?

    (x ?: materialize()).foo()
    (x ?: materialize()).bar()
    (x ?: materialize()).extFoo<Any>()
    (x ?: materialize()).extBar<Any>()
}
