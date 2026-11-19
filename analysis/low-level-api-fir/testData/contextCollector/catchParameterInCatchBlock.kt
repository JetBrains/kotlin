fun test(a: Any) {
    try {
        a.hashCode()
    } catch (e: Exception) {
        <expr>e</expr>
    }
}
