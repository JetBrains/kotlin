// FIX: Simplify negated '>' expression to '<='
fun test(n: Int) {
    <caret>!(0 > 1)
}