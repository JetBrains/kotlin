// PROBLEM: none
// ERROR: Assignment operators ambiguity: <br>public operator fun <T> Collection<Int>.plus(element: Int): List<Int> defined in kotlin.collections<br>public inline operator fun <T> MutableCollection<in Int>.plusAssign(element: Int): Unit defined in kotlin.collections
// WITH_RUNTIME
fun test() {
    var list = mutableListOf(1)
    list <caret>+= 2
}