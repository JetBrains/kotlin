// Issue: KT-31734

fun foo() {
    for (@Foo (i: Int) in y) {}
    for (@Foo (i: () -> Unit) in y) {}
    for (@Foo (i) in y) {}
    for (@Foo (i, j) in y) {}
    for (@Foo (i, j: Int) in y) {}
    for (@Foo i: Int in y) {}
    for (@Foo i in y) {}
}
