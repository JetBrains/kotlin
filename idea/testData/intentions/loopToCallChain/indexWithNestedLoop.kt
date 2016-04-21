// WITH_RUNTIME
// INTENTION_TEXT: "Replace with '+= ...filterNot{}.mapIndexed{}'"
fun foo(list: List<String>, target: MutableCollection<Int>) {
    var i = 0
    <caret>for (s in list) {
        for (j in s.indices) {
            if (j == 10) continue
            target.add(i + j)
            i++
        }
    }
}