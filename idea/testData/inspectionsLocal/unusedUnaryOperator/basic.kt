// "Remove unused unary operator" "true"
fun test(foo: Int?): Int {
    val a = 1 + 2
    <caret>+ 3
    return a
}