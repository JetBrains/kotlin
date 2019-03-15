// PROBLEM: none
// WITH_RUNTIME
fun test(map: Map<Int, String>) {
    val s = map.getValue(1)!!<caret>
}