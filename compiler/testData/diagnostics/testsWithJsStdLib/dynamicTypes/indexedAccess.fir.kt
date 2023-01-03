fun foo() {
    val a: dynamic = Any()
    println(a[0])
    println(a[0, 1])

    a[0] = 23
    a[0, 1] = 42
}
