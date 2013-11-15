fun Any?.outer() {
    fun Any?.inner() {
        if (this is String && this@outer is String) {
            <caret>null
        }
    }
}