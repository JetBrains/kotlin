trait T

val v: T? = object : T {
    fun f() {
        val <error>v</error> = 1
        val <error>v</error> = 1
        <caret>
    }
}
