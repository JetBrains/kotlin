// "Make 'foo' 'abstract'" "false"
// ACTION: Add function body
// ACTION: Make internal
// ACTION: Make private
// ERROR: Function 'foo' without a body must be abstract


object O {
    <caret>fun foo()
}
/* FIR_COMPARISON */
