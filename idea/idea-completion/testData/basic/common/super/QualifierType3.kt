// FIR_COMPARISON
open class AA

interface AI

class B : AA(), AI {
    fun foo() {
        super<A<caret>
    }
}

// EXIST: AA
// EXIST: AI
// NOTHING_ELSE