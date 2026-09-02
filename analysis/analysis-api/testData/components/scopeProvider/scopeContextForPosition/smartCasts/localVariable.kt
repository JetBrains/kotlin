fun test() {
    val p: Any = "foo"
    if (p !is String) {
        return
    }

    <expr>Unit</expr>
}