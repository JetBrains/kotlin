// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-77181
// LANGUAGE: +NestedTypeAliases

@Target(
    AnnotationTarget.TYPE, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.CLASS, AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.TYPEALIAS
)
annotation class Anno(val value: Int)

const val CONST = 1

class TopLevelClass {
    val CONST = "str"

    @Anno(CONST)
    typealias A = @Anno(CONST) String

    @Anno(<!ARGUMENT_TYPE_MISMATCH, NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>CONST<!>)
    <!WRONG_MODIFIER_TARGET!>inner<!> typealias B = @Anno(<!ARGUMENT_TYPE_MISMATCH, NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>CONST<!>) String
}
