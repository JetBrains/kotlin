// FLOW: OUT

fun foo(<caret>n: Int) {
    val x = n

    val y: Int
    y = n

    bar(n)
}

fun bar(m: Int) {

}