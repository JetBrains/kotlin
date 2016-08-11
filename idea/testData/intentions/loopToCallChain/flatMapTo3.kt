// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'flatMapTo(){}'"
// INTENTION_TEXT_2: "Replace with 'asSequence().flatMapTo(){}'"
fun foo(list: List<String>): List<String> {
    val target = createCollection()
    <caret>for (s in list) {
        for (line in s.lines()) {
            target.add(line)
        }
    }
    return target
}

fun createCollection() = java.util.ArrayList<String>()