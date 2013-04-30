// "Cast expression 'x' to '() -> Int'" "true"
fun foo(x: Any): () -> Int {
    return x as () -> Int<caret>
}
