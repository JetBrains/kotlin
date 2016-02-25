// "Change 'AA.contains' function return type to 'Int'" "false"
// ACTION: Change 'AAA.g' function return type to 'Boolean'
// ACTION: Convert to expression body
// ACTION: Replace overloaded operator with function call
// ERROR: Type mismatch: inferred type is Boolean but Int was expected
interface A {
    operator fun contains(i: Int): Boolean
}

open class AA {
    operator fun contains(i: Int) = true
}

class AAA: AA(), A {
    fun g(): Int {
        return 3 i<caret>n this
    }
}