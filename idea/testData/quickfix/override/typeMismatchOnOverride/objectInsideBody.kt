// "Change 'prop' type to 'Int'" "true"
// ERROR: Null can not be a value of a non-null type kotlin.Int
interface Test<T> {
    val prop : T
}

class Other {
    fun doTest() {
        val some = object: Test<Int> {
            override val <caret>prop = null
        }
    }
}