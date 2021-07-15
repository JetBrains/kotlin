// LANGUAGE: -ApproximateIntegerLiteralTypesInReceiverPosition
// ISSUE: Kt-47447, KT-47729

fun takeLong(x: Long) {}

object Foo {
    var longProperty: Long = 0

    infix fun infixOperator(x: Long) {}
}

// Should be warning in all places
fun test() {
    takeLong(<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1 + 1<!>)
    takeLong((<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1 + 1<!>))
    Foo.longProperty = <!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1 + 1<!>
    Foo.longProperty = (<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1 + 1<!>)
    Foo infixOperator <!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1 + 1<!>
    Foo infixOperator (<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1 + 1<!>)
}
