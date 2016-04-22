// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'mapNotNullTo(){}'"
fun foo(list: List<String?>, target: MutableList<Int>) {
    <caret>for (s in list) {
        val length = s?.length
        if (length == null) continue
        target.add(length)
    }
}