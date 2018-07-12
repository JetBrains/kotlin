// WITH_RUNTIME
fun test(s: String) {
    with<caret> (s, {
        println("")
    })
}