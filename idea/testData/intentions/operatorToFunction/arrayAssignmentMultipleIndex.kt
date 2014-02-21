// ERROR: Unresolved reference: array
fun foo(b: Int) {
    var a = array(1, 2, 3, 4, 5)
    a<caret>[2, 3] = b
}
