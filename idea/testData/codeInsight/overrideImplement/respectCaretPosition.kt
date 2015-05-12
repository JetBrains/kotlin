interface Test {
    public open fun test()
    protected open val testProp : Int
}

class SomeTest : Test {
    val hello = 12
    <caret>
    /**
     * test
     */
    fun some() {

    }
}
