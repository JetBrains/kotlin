// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'map{}.map{}.firstOrNull{}'"
// INTENTION_TEXT_2: "Replace with 'asSequence().map{}.map{}.firstOrNull{}'"
fun foo(list: List<String>, o: Any) {
    var result: Any? = ""
    if (o is CharSequence) {
        result = null
        <caret>for (s in list) {
            val a = s.length + (o as String).capitalize().hashCode()
            val x = a * o.length
            if (x > 1000) {
                result = x
                break
            }
        }
    }
}