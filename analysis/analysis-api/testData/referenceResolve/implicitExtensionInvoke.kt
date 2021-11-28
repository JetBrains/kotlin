fun foo(a: Int) {
    <caret>a()
}

operator fun Int.invoke() {}