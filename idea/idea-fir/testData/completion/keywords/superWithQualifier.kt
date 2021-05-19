// FIR_IDENTICAL
// FIR_COMPARISON
class X: List<Int>, Comparable<Int> {
    init {
        sup<caret>
    }
}

// EXIST: super
// EXIST: super<List>
// EXIST: super<Comparable>
// NOTHING_ELSE
