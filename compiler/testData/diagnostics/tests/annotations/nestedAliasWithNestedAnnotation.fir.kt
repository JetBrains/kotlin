@Target(AnnotationTarget.TYPE, AnnotationTarget.TYPEALIAS, AnnotationTarget.TYPE_PARAMETER)
annotation class Anno(val position: String)

interface OriginalInterface<T> {
    <!TOPLEVEL_TYPEALIASES_ONLY!>@Anno("alias $<!UNRESOLVED_REFERENCE!>prop<!>")
    typealias NestedTypeAlias <@Anno("type param $<!UNRESOLVED_REFERENCE!>prop<!>") A : <!BOUND_ON_TYPE_ALIAS_PARAMETER_NOT_ALLOWED!>@Anno("bound $<!UNRESOLVED_REFERENCE!>prop<!>") Number<!>> = @Anno("type $<!UNRESOLVED_REFERENCE!>prop<!>") OriginalInterface<A><!>

    companion object {
        private const val prop = 0
    }
}
