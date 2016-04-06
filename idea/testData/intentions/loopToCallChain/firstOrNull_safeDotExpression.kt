// WITH_RUNTIME
fun foo(list: List<String?>) {
    var result: String? = null
    <caret>for (s in list) {
        if (s != "") {
            result = s?.substring(1)
            break
        }
    }
}
