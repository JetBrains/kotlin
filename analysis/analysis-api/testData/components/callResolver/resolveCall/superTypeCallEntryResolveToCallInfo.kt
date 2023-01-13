interface SimpleClass() : <expr>java.lang.Object()</expr>, I {
    fun foo() : String = "610" + toString()

    override fun toString() : String { return foo() }
}

interface I {}