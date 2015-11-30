// WITH_RUNTIME
fun foo() {
    <caret>assert(true, {
        if (true) "text" else return
    })
}