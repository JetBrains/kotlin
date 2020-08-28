package a

var <caret>test: String
    get() = ""
    set(value: String) {
        val aFoo: Foo = Foo()
        val bFoo: b.Foo = b.Foo()
        val cFoo: c.Foo = c.Foo()
        val aBar: Foo.Bar = Foo.Bar()
        val bBar: b.Foo.Bar = b.Foo.Bar()
        val cBar: c.Foo.Bar = c.Foo.Bar()

        fun foo(u: Int) {
            class T(val t: Int)
            object O {
                val t: Int = 1
            }

            val v = T(u).t + O.t
            println(v)
        }

        foo(1)
    }

class Test {
    fun foo() {
        test
    }
}
