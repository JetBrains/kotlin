// PROBLEM: 'equals' should take 'Any?' as its argument
// FIX: none
class Foo {
    fun <caret>equals(other: Foo?): Boolean {
        return true
    }
}
