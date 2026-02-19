package bar

annotation class Anno(val value: String)

fun foo() {
    class Local {
        @Anno(""
        <expr>fun foo(i: Int) {}</expr>
    }
}
