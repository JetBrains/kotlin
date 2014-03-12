// IS_APPLICABLE: false
class Foo {
    fun Bar(y: Int, x: Int): Int {
        if<caret> (x < y) {
            return 1
        }
        else {
            if (x > y) {
                return 2
            }
            else {
                return 3
            }
        }
    }
}