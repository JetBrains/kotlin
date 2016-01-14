// INTENTION_TEXT: "Replace with '?: error(...)'"
// WITH_RUNTIME

class C {
    fun error(message: String) { }

    fun foo(p: Array<String?>) {
        val v = p[0]
        <caret>assert(v != null) { "message" }
    }
}
