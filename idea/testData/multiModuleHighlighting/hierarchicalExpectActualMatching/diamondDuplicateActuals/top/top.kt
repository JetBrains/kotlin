package sample

expect class <!AMBIGUOUS_ACTUALS("Class 'A'", "left.kt, bottom.kt"), AMBIGUOUS_ACTUALS("Class 'A'", "left.kt, bottom.kt")!>A<!> {
    fun <!AMBIGUOUS_ACTUALS("Function 'foo'", "left.kt, bottom.kt"), AMBIGUOUS_ACTUALS("Function 'foo'", "left.kt, bottom.kt")!>foo<!>(): Int
}