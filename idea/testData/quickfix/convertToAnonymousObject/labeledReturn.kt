// "Convert to anonymous object" "true"
interface I {
    fun foo(): String
}

fun test() {
    <caret>I {
        return@I ""
    }
}
