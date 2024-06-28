@Target(AnnotationTarget.TYPE, AnnotationTarget.TYPEALIAS, AnnotationTarget.TYPE_PARAMETER)
annotation class Anno(val position: String)

fun foo() {
    class OriginalClass<T> {
        val prop = 0

        <!TOPLEVEL_TYPEALIASES_ONLY!>@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"alias $prop"<!>)
        typealias NestedTypeAlias <@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"type param $prop"<!>) A : <!BOUND_ON_TYPE_ALIAS_PARAMETER_NOT_ALLOWED!>@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"bound $prop"<!>) Number<!>> = @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"type $prop"<!>) OriginalClass<A><!>
    }
}
