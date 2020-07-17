// PROBLEM: none

class Test1<T: Any> {
    private inner class Cell
    fun check(o: Any) {
        (o as? Test1<caret><*>.Cell)
    }
}
