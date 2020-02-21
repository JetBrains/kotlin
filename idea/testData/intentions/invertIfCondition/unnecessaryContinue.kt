// WITH_RUNTIME
fun test() {
    for (x in "abc") {
        <caret>if (x == 'a') continue
        println("else")
    }
}