// Anno
// JVM_TARGET: 1.6
// FULL_JDK
// SKIP_SANITY_TEST
// SKIP_IDE_TEST

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.TYPE)
@MustBeDocumented
@Repeatable
annotation class Anno(val i: Int)
