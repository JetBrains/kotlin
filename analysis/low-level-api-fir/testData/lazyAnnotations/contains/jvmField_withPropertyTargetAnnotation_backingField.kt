// QUERY: contains: kotlin.jvm/JvmField
// WITH_STDLIB
// RESOLVE_PROPERTY_PART: BACKING_FIELD

@property:Anno
@JvmField
var vari<caret>able: Int = 0

@Target(AnnotationTarget.PROPERTY)
annotation class Anno
