// "Create parameter 'x'" "false"
// ERROR: Unresolved reference: x
// ACTION: Create property 'x'

enum class E(n: Int) {
    X: E(<caret>x)
}