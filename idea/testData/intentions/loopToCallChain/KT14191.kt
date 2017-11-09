// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filterNotNull().sum()'"
// INTENTION_TEXT_2: "Replace with 'asSequence().filterNotNull().sum()'"
fun f(list: List<Int?>): Int{
    var r = 0
    <caret>for (d in list) {
        if (d != null) {
            r += d
        }
    }
    return r
}