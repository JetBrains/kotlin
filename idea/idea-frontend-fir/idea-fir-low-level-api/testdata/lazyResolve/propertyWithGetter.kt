fun resolveMe() {
    receive(withGetter)
}

fun receive(value: Int){}

val withGetter: Int
    get() = 42