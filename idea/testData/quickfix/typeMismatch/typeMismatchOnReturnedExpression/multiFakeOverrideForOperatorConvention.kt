// "Change 'AA.contains' function return type to 'Int'" "false"
// ACTION: Change 'AAA.g' function return type to 'Boolean'
// ACTION: Convert to expression body
// ACTION: Replace overloaded operator with function call
// ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>kotlin.Int</td></tr><tr><td>Found:</td><td>kotlin.Boolean</td></tr></table></html>
interface A {
    fun contains(i: Int): Boolean
}

open class AA {
    fun contains(i: Int) = true
}

class AAA: AA(), A {
    fun g(): Int {
        return 3 i<caret>n this
    }
}