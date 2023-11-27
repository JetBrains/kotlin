@Target(AnnotationTarget.TYPE, AnnotationTarget.TYPEALIAS, AnnotationTarget.TYPE_PARAMETER)
annotation class Anno(val position: String)

fun foo() {
    class OriginalClass<T> {
        val prop = 0

        <!TOPLEVEL_TYPEALIASES_ONLY!>@Anno("alias $<!UNRESOLVED_REFERENCE!>prop<!>")
        typealias NestedTypeAlias <@Anno("type param $<!UNRESOLVED_REFERENCE!>prop<!>") A : <!BOUND_ON_TYPE_ALIAS_PARAMETER_NOT_ALLOWED!>@Anno("bound $<!UNRESOLVED_REFERENCE!>prop<!>") Number<!>> = @Anno("type $<!UNRESOLVED_REFERENCE!>prop<!>") OriginalClass<A><!>
    }
}
