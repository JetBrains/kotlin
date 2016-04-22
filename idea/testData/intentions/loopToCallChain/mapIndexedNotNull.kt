// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'mapIndexedNotNull{}.mapTo(){}'"
fun foo(list: List<String?>, target: MutableList<String>) {
    <caret>for ((index, s) in list.withIndex()) {
        val length = s?.substring(index)?.length
        if (length == null) continue
        target.add(length.toString())
    }
}