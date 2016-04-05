// WITH_RUNTIME
fun foo(list: List<String>): String? {
    <caret>for (s in list) {
        if (s.isEmpty()) continue
        if (s.length < 10 && s != "abc") {
            if (s == "def") continue
            val s1 = s + "x"
            return s1
        }
    }
    return null
}