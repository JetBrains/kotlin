// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filterNotNull().count()'"
// INTENTION_TEXT_2: "Replace with 'asSequence().filterNotNull().count()'"
fun f11(list: List<Any?>): Int{
    var objs = 0
    <caret>for (d in list) {
        if (d != null) {
            objs++
        }
    }
    return objs
}