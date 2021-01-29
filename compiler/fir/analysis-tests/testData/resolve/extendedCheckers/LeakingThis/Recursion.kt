class Recursion {
    private val s: String
    fun foo() {
        bar()
        <!MAY_BE_NOT_INITIALIZED!>s<!>.length
    }

    fun bar() {
        foo()
    }

    init {
        <!LEAKING_THIS!>foo()<!>
        s = ""
    }
}
