// FIX: Replace negated '>=' operation with '<'
fun test(n: Int) {
    <caret>!(0 >= 1)
}