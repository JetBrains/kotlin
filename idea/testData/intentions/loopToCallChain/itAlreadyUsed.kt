// WITH_RUNTIME
fun foo(list: List<String>, it: Int) {
    var found = false
    <caret>for (s in list) {
        if (s.length > it) {
            found = true
            break
        }
    }
}