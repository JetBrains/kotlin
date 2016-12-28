class Integer(val value: Int) {
    operator fun inc() = Integer(value + 1)
}

fun foo(x: Any, y: Any) {
    x.use()
    y.use()
}

fun main(args : Array<String>) {
    var x = Integer(0)

    for (i in 0..1) {
        val c = Integer(0)
        if (i == 0) x = c
    }

    // x refcount is 1.

    foo(x, ++x)
}

fun Any?.use() {
    var x = this
}