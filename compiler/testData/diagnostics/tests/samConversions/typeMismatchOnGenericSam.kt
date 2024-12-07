// RUN_PIPELINE_TILL: FRONTEND
fun interface MySam<T> {
    fun execute(p0: T)

    fun identity(sam: MySam<T>): MySam<T> = sam

    fun m(p1: T) {
        val y: MySam<T> = identity <!TYPE_MISMATCH!>{ <!EXPECTED_PARAMETER_TYPE_MISMATCH!>it: String<!> -> }<!>
        y.execute(p1)
    }
}