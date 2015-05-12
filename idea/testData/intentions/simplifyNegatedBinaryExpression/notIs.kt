// INTENTION_TEXT: Simplify negated '!is' expression to 'is'
fun test(n: Int) {
    <caret>!(0 !is Int)
}