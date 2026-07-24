fun main() {
    -(l2@ <expr>+(@Anno() (-(-(+2))))</expr>)
}

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.EXPRESSION)
annotation class Anno
