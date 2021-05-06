// "Change parameter 'f' type of function 'foo' to '() -> String'" "true"
fun foo(f: () -> Int) {
    foo {
        ""<caret>
    }
}
/* IGNORE_FIR */
