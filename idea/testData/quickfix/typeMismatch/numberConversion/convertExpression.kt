// "Convert expression to 'Int'" "true"
fun foo() {
    bar("1".toLong()<caret>)
}

fun bar(l: Int) {
}