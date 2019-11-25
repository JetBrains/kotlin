// "Change to var" "true"
object A {
    const val A = 1

    fun foo() {
        <caret>A = 10
    }
}