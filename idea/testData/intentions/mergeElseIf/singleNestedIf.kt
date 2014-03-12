class Foo {
    fun Bar(y: Int, x: Int): Int {
        if (x < y) {
            return 1
        }
        <caret>else {
            if (x > y) {
                return 2
            }
        }
    }
}