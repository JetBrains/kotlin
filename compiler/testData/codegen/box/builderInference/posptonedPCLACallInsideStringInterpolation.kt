// ISSUE: KT-65341
class Controller<K> {
    fun yield(k: K) {}
}

fun <T> generate(lambda: Controller<T>.() -> Unit) {}

fun bar(x: String) {}

var v: String = "OK"

fun foo0(x: String?) {
    generate {
        bar("$v abc${x!!}")

        yield("")
    }
}

fun foo1(x: String?) {
    generate {
        bar("$v abc${when { x != null -> x else -> null}}")

        yield("")
    }
}

fun <E> id(e: E): E = e

fun foo2(x: String?) {
    generate {
        bar("$v abc${id(this)}")

        yield("")
    }
}

fun box(): String {
    foo0(null)
    foo1(null)
    foo2(null)

    return "OK"
}
