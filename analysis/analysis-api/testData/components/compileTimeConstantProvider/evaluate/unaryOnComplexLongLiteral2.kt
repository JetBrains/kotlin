fun main() {
    -(l1@ <expr>+(@Anno() (-(-(+1L))))</expr>)
}

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.EXPRESSION)
annotation class Anno
