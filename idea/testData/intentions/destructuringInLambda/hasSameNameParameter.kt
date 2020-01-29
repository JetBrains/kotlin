// WITH_RUNTIME
data class Event(val timestamp: Int, val otherVar: Int = 1, val otherVar2: String = "")

val listA = listOf(Event(1), Event(2), Event(3))
val listB = listOf(Event(1), Event(2), Event(3))

fun test2() {
    listA.zip(listB) { (timestamp), <caret>it2 -> timestamp + it2.timestamp }
}