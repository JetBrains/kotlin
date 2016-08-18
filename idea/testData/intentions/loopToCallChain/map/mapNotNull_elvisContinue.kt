// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'mapNotNull{}.mapTo(){}'"
// INTENTION_TEXT_2: "Replace with 'asSequence().mapNotNull{}.mapTo(){}'"
fun foo(list: List<String?>, target: MutableList<String>) {
    <caret>for (s in list) {
        val length = s?.length ?: continue
        target.add(length.toString())
    }
}