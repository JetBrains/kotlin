// MEMBER_NAME_FILTER: resolveMe
package second

@Target(
    AnnotationTarget.TYPE,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.PROPERTY_GETTER,
)
annotation class Anno(val position: String)

abstract class S<caret>ubClass: AbstractClass<@Anno("type param: $constant") List<@Anno("nested type param: $constant") Collection<@Anno("nested nested type param: $constant")String>>>()

abstract class AbstractClass<T> {
    fun explicitType(): @Anno("explicit type $constant") List<@Anno("nested explicit type $constant") List<@Anno("nested nested explicit type $constant") T>>? = null

    @property:Anno("property $constant")
    @get:Anno("get $constant")
    @set:Anno("set $constant")
    @setparam:Anno("set $constant")
    @field:Anno("field $constant")
    var resolveMe = explicitType()

    companion object {
        private const val constant = "str"
    }
}
