interface I {
    fun <caret>x()
}

inline class Foo(val value: Int) : I {
    override fun x() {}
}

// REF: (in Foo).x()