fun foo(p: String?): () -> String {
    if (p == null) {
        return {
            println()
            "a"
        }
    }
    <caret>p.length()
}