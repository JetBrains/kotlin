interface Tr<T> {
    val v: T
}

class C : Tr<String> {
    override val v = "OK"
}

fun box() = C().v
