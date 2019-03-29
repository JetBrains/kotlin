// PROBLEM: none
interface F

val f = object : F {
    fun <caret>equals(other: F?): Boolean {
        return true
    }
}