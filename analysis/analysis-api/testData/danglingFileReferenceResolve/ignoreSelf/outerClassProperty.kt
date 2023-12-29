class Outer {
    val foo: String = "foo"

    inner class Inner {
        fun test() {
            f<caret>oo
        }
    }
}