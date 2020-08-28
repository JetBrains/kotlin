interface I {
    fun foo(i: Int)
}

class C {
    fun create(): I {
        return object : I {
            override fun foo(i: Int) {
                bar {<caret> baz(it) }
            }

            fun bar(f: (Int) -> Unit) {}

            fun baz(i: Int) {}
        }
    }
}