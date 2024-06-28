enum class X {
    A
}

annotation class Anno(val args: A.X)

class B {
    @Anno(X.A)
    fun resolve<caret>Me() {
    }

    @Anno(X.A)
    fun foo() {
    }
}