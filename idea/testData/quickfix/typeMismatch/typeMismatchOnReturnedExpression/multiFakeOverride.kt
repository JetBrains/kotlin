// "Change 'AA.f' function return type to 'Boolean'" "false"
// ACTION: Change 'AAA.g' function return type to 'Int'
// ACTION: Convert to expression body
// ERROR: Type mismatch: inferred type is Int but Boolean was expected
interface A {
    fun f(): Int
}

open class AA {
    fun f() = 3
}

class AAA: AA(), A {
    fun g(): Boolean {
        return f<caret>()
    }
}