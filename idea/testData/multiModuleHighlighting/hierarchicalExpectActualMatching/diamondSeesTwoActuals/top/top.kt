package sample

expect class <!AMBIGUOUS_ACTUALS("Class 'A'", "left.kt, right.kt"), AMBIGUOUS_ACTUALS("Class 'A'", "left.kt, right.kt")!>A<!> {
    fun <!AMBIGUOUS_ACTUALS("Function 'foo'", "left.kt, right.kt"), AMBIGUOUS_ACTUALS("Function 'foo'", "left.kt, right.kt")!>foo<!>(): Int
}