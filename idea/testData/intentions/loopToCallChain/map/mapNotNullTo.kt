// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'mapNotNullTo(){}'"
// IS_APPLICABLE_2: false
fun foo(list: List<String?>, target: MutableList<Int>) {
    <caret>for (s in list) {
        val length = s?.length
        if (length == null) continue
        target.add(length)
    }
}