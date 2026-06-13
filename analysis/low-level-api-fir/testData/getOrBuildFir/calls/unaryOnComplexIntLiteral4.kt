fun main() {
    -(l2@ +(@Anno() (-<expr>(-(+2))</expr>)))
}

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.EXPRESSION)
annotation class Anno
