// MOVE: down
fun takeLamb(f: () -> Unit) {}
fun foo() {}
takeLamb {
    <caret>foo()
}

