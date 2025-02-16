// RUN_PIPELINE_TILL: FRONTEND
@Target(AnnotationTarget.TYPE, AnnotationTarget.TYPEALIAS, AnnotationTarget.TYPE_PARAMETER)
annotation class Anno(val position: String)

interface OriginalInterface<T> {
    <!UNSUPPORTED_FEATURE!>@Anno("alias $prop")
    typealias NestedTypeAlias <@Anno("type param $prop") A : <!BOUND_ON_TYPE_ALIAS_PARAMETER_NOT_ALLOWED!>@Anno("bound $prop") Number<!>> = @Anno("type $prop") OriginalInterface<A><!>

    companion object {
        private const val prop = 0
    }
}
