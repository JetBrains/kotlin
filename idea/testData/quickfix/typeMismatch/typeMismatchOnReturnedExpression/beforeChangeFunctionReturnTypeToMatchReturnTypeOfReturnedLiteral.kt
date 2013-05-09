// "Change 'foo' function return type to '() -> Any'" "true"
fun foo(x: Any): () -> Int {
    return {x<caret>}
}