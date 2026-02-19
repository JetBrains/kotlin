// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm
// WITH_STDLIB

sealed interface Subject
class IntEntry(val value: Int) : Subject

fun box(): String {
    val subject: Subject = IntEntry(1)

    if (subject is IntEntry) {
        val (value) = subject
        if (value != 1) return "FAIL"
    }

    val whenText = when (subject) {
        is IntEntry -> { val (whenValue = value) = subject; "a:$whenValue" }
    }
    if (whenText != "a:1") return "FAIL"

    val inputItems: List<Subject> = listOf(IntEntry(1), IntEntry(2))

    var accumulatedValue = 0
    for ((itemIndex = index, item = value) in inputItems.withIndex()) {
        if (item is IntEntry) {
            val (current = value) = item
            if (current != itemIndex + 1) return "FAIL"
            accumulatedValue += current
        }
    }
    if (accumulatedValue != 3) return "FAIL"

    val renderedLabels = inputItems.mapNotNull { it ->
        if (it is IntEntry) { val (current = value) = it; "b:$current" } else null
    }
    if (renderedLabels.joinToString() != "b:1, b:2") return "FAIL"

    val aggregatedByParams = inputItems
        .filterIsInstance<IntEntry>()
        .sumOf { (value) -> value }
    if (aggregatedByParams != 3) return "FAIL"

    return "OK"
}
