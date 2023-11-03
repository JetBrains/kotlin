// MEMBER_NAME_FILTER: resolveMe
package second

@Target(AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
annotation class Anno(val position: String)
const val constant = "str"

abstract class S<caret>ubClass: AbstractClass<@Anno("type argument: $constant") List<@Anno("nested type argument: $constant") Collection<@Anno("nested nested type argument: $constant")String>>>()

abstract class AbstractClass<T> {
    fun explicitType(): @Anno("explicit type $constant") List<@Anno("nested explicit type $constant") List<@Anno("nested nested explicit type $constant") T>>? = null

    @Anno("function $constant")
    fun <@Anno("type param $constant") F : @Anno("bound type $constant") List<@Anno("nested bound type $constant") List<@Anno("nested nested bound type $constant") String>>> @receiver:Anno("receiver $constant") @Anno("receiver type $constant") List<@Anno("nested receiver type $constant") Collection<@Anno("nested nested receiver type $constant") T>>.resolveMe(@Anno("param $constant") param: @Anno("param type $constant") List<@Anno("nested param type $constant") Collection<@Anno("nested nested param type $constant") T>>) = explicitType()
}
