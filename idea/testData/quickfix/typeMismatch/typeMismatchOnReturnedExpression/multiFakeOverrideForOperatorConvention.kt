// "Change return type of called function 'AA.contains' to 'Int'" "false"
// ACTION: Change return type of enclosing function 'AAA.g' to 'Boolean'
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