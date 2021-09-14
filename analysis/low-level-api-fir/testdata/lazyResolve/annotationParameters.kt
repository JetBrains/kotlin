enum class X {
    A
}

annotation class Anno(val args: A.X)

class B {
    @Anno(X.A)
    fun resolveMe() {
    }

    @Anno(X.A)
    fun foo() {
    }
}