// See KT-5113
enum class E {
    A,
    B
}

class Outer(e: E) {
    private val prop: Int
    init {
        <!DEBUG_INFO_IMPLICIT_EXHAUSTIVE!>when(e ) {
            // When is exhaustive, property is always initialized
            E.A -> prop = 1
            E.B -> prop = 2
        }<!>
    }
}