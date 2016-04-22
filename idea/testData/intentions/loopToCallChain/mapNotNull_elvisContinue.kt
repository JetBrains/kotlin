// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'mapNotNull{}.mapTo(){}'"
fun foo(list: List<String?>, target: MutableList<String>) {
    <caret>for (s in list) {
        val length = s?.length ?: continue
        target.add(length.toString())
    }
}