class C {
    fun <caret>f(p1: Int, p2: Int) {
        println(p1)
        nonUnit(p2)
    }

    fun nonUnit(p: Int): Int = p

    fun <T> doIt(p: () -> T): T = TODO()

    fun x() = doIt<Unit> { f(9, 10) }
}
