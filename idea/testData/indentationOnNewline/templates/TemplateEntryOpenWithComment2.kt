fun a() {
    val b = 3
    "" +
    "${
        // smth<caret>
        a
    }"
}
