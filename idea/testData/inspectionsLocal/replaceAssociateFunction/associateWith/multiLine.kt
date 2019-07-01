// PROBLEM: Replace 'associate' with 'associateWith'
// FIX: Replace with 'associateWith'
// WITH_RUNTIME
fun getValue(i: Int): String = ""

fun test() {
    listOf(1).<caret>associate {
        val value = getValue(it)
        it to value
    }
}