// !LANGUAGE: +ContextReceivers
// IGNORE_BACKEND_FIR: JVM_IR

interface Lazy<T>

context(Lazy<Int>, Lazy<CharSequence>)
fun test1() {}

context(Lazy<T>)
fun <T> Lazy<Int>.test2() {}

context(Lazy<Lazy<T>>)
fun <T> Lazy<Int>.test3() {}

fun <T> f(lazy1: Lazy<Int>, lazy2: Lazy<CharSequence>, lazyT: Lazy<T>, lazyLazyT: Lazy<Lazy<T>>) {
    with(lazy1) {
        with(lazy2) {
            test1()
            test2()
        }
    }
    with(lazy2) {
        with(lazy1) {
            test1()
            test2()
        }
    }
    with(lazyT) {
        with(lazy1) {
            test2()
        }
    }
    with(lazyLazyT) {
        with(lazy1) {
            test2()
            test3()
        }
    }
    with(lazy1) {
        with(lazyLazyT) {
            test2()
            test3()
        }
    }
}