fun main() {
    -(l1@ +(@Anno() (-<expr>(-(+1L))</expr>)))
}

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.EXPRESSION)
annotation class Anno
