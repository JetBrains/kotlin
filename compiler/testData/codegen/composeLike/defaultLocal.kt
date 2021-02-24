fun box(): String {
    fun foo(s: String = "O") = s
    return foo() + foo("K")
}
