// IS_APPLICABLE: false

interface A : () -> Unit {
    fun f() {
        super<<caret>>.contains(1) // Type a space between <>
    }
}