// !RENDER_DIAGNOSTICS_MESSAGES

package sample

expect class <!AMBIGUOUS_ACTUALS("Class 'A'; bottom, left")!>A<!> {
    fun <!AMBIGUOUS_ACTUALS("Function 'foo'; bottom, left")!>foo<!>(): Int
}