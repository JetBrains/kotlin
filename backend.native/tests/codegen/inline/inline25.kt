inline fun foo(block: String.() -> Unit) {
    "Ok".block()
}

inline fun bar(block: (String) -> Unit) {
    foo(block)
}

inline fun baz(block: String.() -> Unit) {
    block("Ok")
}

fun main(args: Array<String>) {
    bar {
        println(it)
    }

    baz {
        println(this)
    }
}
