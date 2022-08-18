interface WithInvoke {
    operator fun invoke() {}
}

fun foo(parameter: Any) {
    if (parameter is WithInvoke) {
        <caret>parameter()
    }
}
