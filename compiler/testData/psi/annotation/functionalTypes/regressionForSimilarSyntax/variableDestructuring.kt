// Issue: KT-31734

fun foo() {
    val @Foo (i) = Pair(1, 2)
    var @Foo (i: () -> Unit) = Pair(1, 2)
    var @Foo (i: Int) = Pair(1, 2)
    val @Foo (i, j) = Pair(1, 2)
    val @Foo (i, j: Int) = Pair(1, 2)
}
