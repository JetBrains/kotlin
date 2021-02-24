
class Outer<OP> {
    inner class Inner<IP>

    fun <T> withInner(block: Inner<T>.() -> String) = Inner<T>().block()
}

fun <TT> withOuter(block: Outer<TT>.() -> String) = Outer<TT>().block()

fun box() = withOuter<Int> {
    withInner<Boolean> {
        "OK"
    }
}
