// MEMBER_NAME_FILTER: resolveMe
package second

@Target(
    AnnotationTarget.TYPE,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.TYPE_PARAMETER,
)
annotation class Anno(val position: String)
const val constant = "str"

abstract class S<caret>ubClass: AbstractClass<@Anno("type argument: $constant") List<@Anno("nested type argument: $constant") Collection<@Anno("nested nested type argument: $constant")String>>>()

abstract class AbstractClass<T> {
    @property:Anno("property $constant")
    @get:Anno("get $constant")
    @set:Anno("set $constant")
    @setparam:Anno("set $constant")
    abstract var <@Anno("type param $constant") F : @Anno("bound $constant") List<@Anno("nested bound $constant") Collection<@Anno("nested nested bound $constant") T>>> @receiver:Anno("receiver $constant") @Anno("receiver type $constant") Pair<@Anno("nested left receiver type $constant") List<@Anno("nested nested left receiver type $constant") T>, @Anno("nested right receiver type $constant") List<@Anno("nested nested right receiver type $constant") F>>.resolveMe: @Anno("return type $constant") List<@Anno("nested return type $constant") Collection<@Anno("nested nested return type $constant") T>>
}
