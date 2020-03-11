fun <X : Any> foo(x: X): String {
    fun <Y : Any> bar(y: Y) =
            x.toString() + y.toString()

    return bar("K")
}

fun box() = foo("O")