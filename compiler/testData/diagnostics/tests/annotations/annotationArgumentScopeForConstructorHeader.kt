// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ImprovedResolutionInSecondaryConstructors
// ISSUE: KT-77180
// ISSUE: KT-77276
@Retention(AnnotationRetention.SOURCE)
@Target(
    AnnotationTarget.TYPE, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.CLASS, AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.TYPEALIAS,AnnotationTarget.EXPRESSION,
)
annotation class Anno(val value: Int)

const val CONST = 1

abstract class MySuper(s: String)

class TopLevelClass @Anno(CONST)/*Error*/ constructor(@Anno(CONST) x: @Anno(CONST) String) : MySuper(@Anno(CONST) "") {
    @Anno(CONST)/*Error*/
    constructor(@Anno(<!ARGUMENT_TYPE_MISMATCH, INSTANCE_ACCESS_BEFORE_SUPER_CALL, NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>CONST<!>)/*Error*/ i: @Anno(<!ARGUMENT_TYPE_MISMATCH, INSTANCE_ACCESS_BEFORE_SUPER_CALL, NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>CONST<!>)/*Error*/ Int) : this(@Anno(<!ARGUMENT_TYPE_MISMATCH, INSTANCE_ACCESS_BEFORE_SUPER_CALL, NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>CONST<!>) "")

    val CONST = "str"
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, const, integerLiteral, primaryConstructor,
propertyDeclaration, secondaryConstructor, stringLiteral */
