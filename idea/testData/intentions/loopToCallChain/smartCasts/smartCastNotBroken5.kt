// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'map{}.map{}.filterTo(){}'"
// INTENTION_TEXT_2: "Replace with 'asSequence().map{}.map{}.filterTo(){}'"
fun foo(list: List<String>, o: Any, result: MutableCollection<Int>) {
    if (o is CharSequence) {
        <caret>for (s in list) {
            val a = s.length + (o as String).capitalize().hashCode()
            val x = a * o.length
            if (x > 1000) {
                result.add(x)
            }
        }
    }
}