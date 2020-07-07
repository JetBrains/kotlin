// MODE: return
@Target(AnnotationTarget.EXPRESSION)
annotation class Some

fun test() {
    run {
        val files: Any? = null
        @Some
        12<# ^run #>
    }

    run {
        val files: Any? = null
        @Some 12<# ^run #>
    }
}