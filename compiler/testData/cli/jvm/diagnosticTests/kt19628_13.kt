@Target(AnnotationTarget.PROPERTY_GETTER)
annotation class Anno(val value: kotlin.reflect.KClass<*>)

@Suppress("ANNOTATION_TARGETS_NON_EXISTENT_ACCESSOR")
class Data(
    @get:Anno(CollapsedStringAdapter::class)
    var value: String?
) {
    @get:Anno(CollapsedStringAdapter::class)
    private val b: String = ""
}
