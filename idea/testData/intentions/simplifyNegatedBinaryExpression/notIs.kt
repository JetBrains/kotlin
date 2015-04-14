// INTENTION_TEXT: Simplify negated '!is' expression to 'is'
fun test(n: Int) {
    !(0<caret> !is Int)
}