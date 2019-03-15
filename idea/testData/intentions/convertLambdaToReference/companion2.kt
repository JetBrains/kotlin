class C {
    companion object {
        fun foo(s: String) = 1
    }
    val f = {<caret> s: String -> foo(s) }
}