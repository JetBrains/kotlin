// FALSE
class Test {
    val more : Int = 0
    val test : Int
        get() {
            <caret>
            return more
        }
}