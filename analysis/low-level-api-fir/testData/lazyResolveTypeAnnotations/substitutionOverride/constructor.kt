// MEMBER_CLASS_FILTER: org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
package second

@Target(AnnotationTarget.TYPE, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.VALUE_PARAMETER)
annotation class Anno(val position: String)
const val constant = "str"

abstract class S<caret>ubClass: AbstractClass<@Anno("type param: $constant") List<@Anno("nested type param: $constant") Collection<@Anno("nested nested type param: $constant")String>>>()

abstract class AbstractClass<T> {
    @Anno("constructor $constant")
    constructor(@Anno("param $constant") t: @Anno("param type $constant") T)
}
