package sample

expect class <!AMBIGUOUS_ACTUALS("Class 'A'", "bottom.kt, left.kt")!>A<!> {
    fun <!AMBIGUOUS_ACTUALS("Function 'foo'", "bottom.kt, left.kt")!>foo<!>(): Int
}