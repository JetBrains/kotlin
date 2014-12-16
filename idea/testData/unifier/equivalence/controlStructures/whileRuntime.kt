fun foo(a: Int, b: Int) {
    var t = a
    <selection>while (t != (a + b)) {
        println(t)
        t++
    }</selection>

    var u = a
    while (u != a + b) {
        println(t)
        t++
    }

    do {
        println(t)
        t++
    } while (t != a + b)

    while (t != a + b) {
        println((t))
        t++
    }
}