fun test() {
    var p: Any = "foo"
    if (p !is String) {
        return
    }

    <expr>Unit</expr>
}