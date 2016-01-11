fun<T> f(klass: Class<Array<T>>) {
}

fun g() {
    f<String>(<caret>)
}
// ELEMENT: Array