// WITH_RUNTIME
// SIBLING:
fun foo() {
    val (a, b) =
            if (true) {
                <selection>1 + 1</selection>
                1 to 2
            } else {
                2 to 3
            }
}