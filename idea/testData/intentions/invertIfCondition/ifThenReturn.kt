fun foo(): Boolean {
    val x = 2
    <caret>if (x <= 1) {
        bar1()
        bar2()
        return false
    }
    bar1()
    return true
}

fun bar1(){}
fun bar2(){}
