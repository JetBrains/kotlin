sealed interface Base {
    object A : Base
    private object Private : Base
    private class PrivateClass : Base

    fun test(b: Base) {
        <caret>when (b) {
        }
    }
}
