// "Create property 'foo'" "false"
// ACTION: Rename reference
// ACTION: Create function 'foo'
// ERROR: Unresolved reference: foo
fun test(f: (Int) -> Int) {}

fun refer() {
    val v = test(::<caret>foo)
}
