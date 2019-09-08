// "Add missing actual members" "true"
// DISABLE-ERRORS

actual object <caret>O {
    fun <T> hello(): MutableMap<String, T> {
        TODO("not implemented")
    }
}