// PROBLEM: none

interface Callback {
    fun on(id: Int)
}

fun called(id: Int) {
    val <caret>calledId = id
    object : Callback {
        override fun on(id: Int) {
            if (id == calledId) {
            }
        }
    }
}