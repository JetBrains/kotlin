// OPTION: 1
fun foo(n: Int) {
    var m = n
    <caret>while (m > 0) {
        m--
        println(m)
    }
}
