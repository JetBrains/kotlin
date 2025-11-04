// LANGUAGE: +NameBasedDestructuring
// WITH_STDLIB

sealed interface Subject
class IntEntry(val value: Int) : Subject

fun box(): String {
    val subject: Subject = IntEntry(1)

    if (subject is IntEntry) {
        (val value: Int) = subject
        if (value != 1) return "FAIL"
    }


    val whenText = when (subject) {
        is IntEntry -> { (val whenValue: Int = value) = subject; "a:$whenValue" }
    }
    if (whenText != "a:1") return "FAIL"

    val inputItems: List<Subject> = listOf(IntEntry(1), IntEntry(2))

    var accumulatedValue = 0
    for ((val itemIndex: Int = index, val item: Subject = value) in inputItems.withIndex()) {
        if (item is IntEntry) {
            (val current: Int = value) = item
            if (current != itemIndex + 1) return "FAIL"
            accumulatedValue += current
        }
    }
    if (accumulatedValue != 3) return "FAIL"

    val renderedLabels = inputItems.mapNotNull { it ->
        if (it is IntEntry) { (val current: Int = value) = it; "b:$current" } else null
    }
    if (renderedLabels.joinToString() != "b:1, b:2") return "FAIL"

    val aggregatedByParams = inputItems
        .filterIsInstance<IntEntry>()
        .sumOf { (val current: Int = value) -> current }
    if (aggregatedByParams != 3) return "FAIL"

    return "OK"
}
