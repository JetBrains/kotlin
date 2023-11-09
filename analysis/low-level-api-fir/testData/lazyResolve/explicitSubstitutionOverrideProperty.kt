// MEMBER_NAME_FILTER: resolveMe
package second

@Target(
    AnnotationTarget.PROPERTY,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.PROPERTY_GETTER,
)
annotation class Anno(val position: String)
const val constant = "str"

abstract class S<caret>ubClass: AbstractClass<String>()

abstract class AbstractClass<T> {
    fun explicitType(): T? = null

    @property:Anno("property $constant")
    @get:Anno("get $constant")
    @set:Anno("set $constant")
    @setparam:Anno("set $constant")
    @field:Anno("field $constant")
    var resolveMe: T? = explicitType()
}
