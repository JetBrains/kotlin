class Controller<K>

fun <T> generate(lambda: Controller<T>.(T) -> Unit) {}

fun consume(f: Controller<String>) {}
fun consumeString(f: String) {}

fun box(): String {
    generate {
        consume(this)
    }

    generate {
        consumeString(it)
    }

    return "OK"
}
