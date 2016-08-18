// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filterIndexed{}.filterIndexedTo(){}'"
// INTENTION_TEXT_2: "Replace with 'asSequence().filterIndexed{}.filterIndexedTo(){}'"
fun foo(list: List<String>, target: MutableCollection<String>) {
    var j = 0
    <caret>for ((i, s) in list.withIndex()) {
        if (s.length > i) continue
        if (s.length % j == 0) {
            target.add(s)
        }
        j++
    }
}