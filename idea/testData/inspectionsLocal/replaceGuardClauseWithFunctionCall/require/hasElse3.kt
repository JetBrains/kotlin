// WITH_RUNTIME
fun test(flag: Boolean, i: Int) {
    <caret>if (!flag) {
        throw IllegalArgumentException()
    } else if (i == 0) {
        println(0)
    } else {
        println(1)
        println(2)
    }
}