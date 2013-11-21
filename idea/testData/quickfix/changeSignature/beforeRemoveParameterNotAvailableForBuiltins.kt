// "Remove parameter 'other'" "false"
// ERROR: No value passed for parameter other

fun f(d: Boolean) {
    d.or(<caret>)
}