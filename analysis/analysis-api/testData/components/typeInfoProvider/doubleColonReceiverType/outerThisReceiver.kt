class Test {
    fun bar(): Int = 42
    inner class Foo {
        fun m() {
            val f = this@Test:<caret>:bar
        }
    }
}