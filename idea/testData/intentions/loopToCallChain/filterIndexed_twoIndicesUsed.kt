// WITH_RUNTIME
// IS_APPLICABLE: false
// IS_APPLICABLE_2: false
fun foo(list: List<String>, target: MutableCollection<Int>) {
    var j = 0
    <caret>for ((i, s) in list.withIndex()) {
        val x = s.length + i
        if (x < i * j) {
            target.add(x)
        }
        j++
    }
}