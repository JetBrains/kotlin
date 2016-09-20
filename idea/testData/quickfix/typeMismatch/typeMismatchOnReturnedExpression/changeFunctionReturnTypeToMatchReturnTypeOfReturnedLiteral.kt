// "Change return type of current function 'foo' to '() -> Any'" "true"
fun foo(x: Any): () -> Int {
    return {x<caret>}
}