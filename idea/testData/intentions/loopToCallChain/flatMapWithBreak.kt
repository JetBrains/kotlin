// WITH_RUNTIME
fun foo(list: List<String>): String? {
    var result: String? = null
    MainLoop@
    <caret>for (s in list) {
        for (line in s.lines()) {
            if (line.isNotBlank()) {
                result = line
                break@MainLoop
            }
        }
    }
    return result
}