// "Change 'foo' function return type to '() -> Any'" "true"
fun foo(x: Any): () -> Any {
    return {x<caret>}
}