fun main() {
    -(l1@ +(@Anno(33) (-(-(+1L)))))

    -(l2@ +(@Anno(44) (-(-(+2)))))
}

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.EXPRESSION)
annotation class Anno(val v: Int)
