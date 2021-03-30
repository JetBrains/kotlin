// Anno
// JVM_TARGET: 1.6

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.TYPE)
@MustBeDocumented
@Repeatable
annotation class Anno(val i: Int)
