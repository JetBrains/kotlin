class Recursion {
    private val s: String
    fun foo() {
        bar()
        <!LEAKING_THIS!>s<!>.length
    }

    fun bar() {
        foo()
    }

    init {
        foo()
        s = ""
    }
}
