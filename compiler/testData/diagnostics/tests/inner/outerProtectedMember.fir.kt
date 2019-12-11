// KT-2100

interface I {
    val x : String
}

class Foo {
    protected val x : String = ""

    inner class Inner : I {
        override val x : String = this@Foo.x
    }
}
