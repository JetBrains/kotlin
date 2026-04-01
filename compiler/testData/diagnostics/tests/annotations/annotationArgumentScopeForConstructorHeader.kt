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
    constructor(@Anno(CONST)/*Error*/ i: @Anno(CONST)/*Error*/ Int) : this(@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, INSTANCE_ACCESS_BEFORE_SUPER_CALL, TYPE_MISMATCH!>CONST<!>) "")

    val CONST = "str"
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, const, integerLiteral, primaryConstructor,
propertyDeclaration, secondaryConstructor, stringLiteral */
