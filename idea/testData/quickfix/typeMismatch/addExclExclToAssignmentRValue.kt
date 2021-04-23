// "Add non-null asserted (!!) call" "true"
fun test() {
    val s: String? = null
    val z: String = <caret>s
}
