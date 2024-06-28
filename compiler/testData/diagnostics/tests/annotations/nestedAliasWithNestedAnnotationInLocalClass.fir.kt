@Target(AnnotationTarget.TYPE, AnnotationTarget.TYPEALIAS, AnnotationTarget.TYPE_PARAMETER)
annotation class Anno(val position: String)

fun foo() {
    class OriginalClass<T> {
        val prop = 0

        <!TOPLEVEL_TYPEALIASES_ONLY!>@Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"alias $prop"<!>)
        typealias NestedTypeAlias <@Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"type param $prop"<!>) A : <!BOUND_ON_TYPE_ALIAS_PARAMETER_NOT_ALLOWED!>@Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"bound $prop"<!>) Number<!>> = @Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"type $prop"<!>) OriginalClass<A><!>
    }
}
