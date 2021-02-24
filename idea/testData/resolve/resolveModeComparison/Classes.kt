class Foo(val prop: String) {
    class Inner {
        val boo = 42

        fun inner() {}
    }

    fun first(arg1: String) {
        <caret>arg1
    }

    fun second(arg2: Int) {
        arg2
    }
}

class Bar(val baz: Int) {
    fun bee() {
        val local = "abc"
    }
}