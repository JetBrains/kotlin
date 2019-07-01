// INSPECTION_CLASS: org.jetbrains.kotlin.idea.inspections.UnusedEqualsInspection

fun main() {
    val list = java.util.ArrayList<Int>()
    <caret>val x = 0
}

fun <T> Collection<T>.isAny(predicate: (T) -> Boolean): Boolean {
    for (item in this) {
        if (predicate(item)) {
            return true
        }
    }

    return false
}