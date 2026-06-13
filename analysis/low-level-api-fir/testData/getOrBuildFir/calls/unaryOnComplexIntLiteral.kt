fun main() {
    <expr>-(l2@ +(@Anno(33) (-(-(+2)))))</expr>
}

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.EXPRESSION)
annotation class Anno(val v: Int)
