// "Replace with 'tailrec'" "true"

@tailRecursive<caret>
fun foo() {
    if (1 > 2) {
        foo()
    }
}
