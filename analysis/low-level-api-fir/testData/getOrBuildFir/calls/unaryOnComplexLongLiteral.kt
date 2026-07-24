fun main() {
    <expr>-(l1@ +(@Anno(33) (-(-(+1L)))))</expr>
}

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.EXPRESSION)
annotation class Anno(val v: Int)
