package sample

expect class <!AMBIGUOUS_ACTUALS("Class 'A'", "bottom for JVM, left")!>A<!> {
    fun <!AMBIGUOUS_ACTUALS("Function 'foo'", "bottom for JVM, left")!>foo<!>(): Int
}