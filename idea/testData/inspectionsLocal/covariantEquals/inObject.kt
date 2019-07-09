// PROBLEM: 'equals' should take 'Any?' as its argument
// FIX: none
object F {
    fun <caret>equals(other: F?): Boolean {
        return true
    }
}