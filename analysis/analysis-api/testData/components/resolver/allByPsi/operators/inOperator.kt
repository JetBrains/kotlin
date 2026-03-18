class Container {
    operator fun contains(element: Int): Boolean = true
}

fun test() {
    val c = Container()
    1 in c
    2 !in c
}
