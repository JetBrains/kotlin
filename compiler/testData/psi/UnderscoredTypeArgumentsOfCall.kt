fun main() {
    val x = foo<Int, _>()
    val x = foo<_, _, _>()
    val x = foo<_, _, Int>()
    val x = foo<_>()
}
