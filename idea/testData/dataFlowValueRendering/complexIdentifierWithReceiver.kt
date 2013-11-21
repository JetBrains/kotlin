fun C.outer(a: Any?) {
    if (this.x is String) {
        <caret>null
    }
}

class C {
    val x: Any? = null
}