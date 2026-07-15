// Anno
// STDLIB_JDK8
// FULL_JDK

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.TYPE)
@MustBeDocumented
@Repeatable
annotation class Anno(val i: Int)

// LIGHT_ELEMENTS_NO_DECLARATION: Anno.class[value]
