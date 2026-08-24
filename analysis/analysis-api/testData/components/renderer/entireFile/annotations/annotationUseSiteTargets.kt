@Target(
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.TYPEALIAS,
)
annotation class Anno

class ConstructorProperties(
    @param:Anno @property:Anno @field:Anno @get:Anno @set:Anno @setparam:Anno var full: Int,
    @Anno private val simple: String,
    @Anno plain: Long,
)

@Anno
@field:Anno
@setparam:Anno
var withDefaultAccessors: Int = 0
    @Anno get
    @Anno set

var withCustomSetter: Int = 0
    set(@Anno value) {
        field = value
    }

@delegate:Anno
val delegated: Int by lazy { 0 }

fun @receiver:Anno String.extension() {}

@Anno
typealias Alias = String
