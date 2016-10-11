fun foo(p: List<String?>) {
    for (i in 1..10) {
        val v = p[i]
        <caret>if (v == null) continue
    }
}