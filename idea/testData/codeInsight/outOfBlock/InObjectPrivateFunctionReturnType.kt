// OUT_OF_CODE_BLOCK: TRUE
// TYPE: 'Int'
// TODO: changes in private functions is still subject to OOCB
object A {
    fun foo(): Int = 12

    private fun bar(): <caret> = foo()
}

// SKIP_ANALYZE_CHECK