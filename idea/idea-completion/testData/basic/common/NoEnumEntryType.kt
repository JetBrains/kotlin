// FIR_COMPARISON
enum class E {
    AAA
    BBB

    fun foo(): <caret>
}

// ABSENT: AAA
// ABSENT: BBB

