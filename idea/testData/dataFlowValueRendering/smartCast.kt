fun outer(a: Any?) {
    if (a is String) {
        <caret>null
    }
}