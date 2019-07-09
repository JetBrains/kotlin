// PROBLEM: Calling non-final function toString in constructor
// FIX: none
enum class Fo(val b: Int) {
    ONE(5) {
        override val x = b
        override fun toString(): String {
            return x.toString()
        }
    };

    abstract val x: Int

    val dos = <caret>toString()
}