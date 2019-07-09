// PROBLEM: 'equals' should take 'Any?' as its argument
// FIX: none
interface F

val f = object : F {
    fun <caret>equals(other: F?): Boolean {
        return true
    }
}