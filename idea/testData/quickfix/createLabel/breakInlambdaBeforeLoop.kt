// "Create label foo@" "false"
// ERROR: The label '@foo' does not denote a loop
// ERROR: Unresolved reference: @foo

fun bar(f: () -> Unit) { }

fun test() {
    while (true) {
        bar { break@<caret>foo }
    }
}