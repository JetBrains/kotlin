// FIR_COMPARISON
class A {
    public fun foo(): Int = 1
    public val bar: Int = 1
}

fun A.ext() {
    <caret>
}

// EXIST: foo
// EXIST: bar
