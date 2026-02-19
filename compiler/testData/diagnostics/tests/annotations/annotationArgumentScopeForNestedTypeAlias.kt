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

    <!TOPLEVEL_TYPEALIASES_ONLY!>@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, TYPE_MISMATCH!>CONST<!>)
    typealias A = @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, TYPE_MISMATCH!>CONST<!>) String<!>

    <!TOPLEVEL_TYPEALIASES_ONLY!>@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, TYPE_MISMATCH!>CONST<!>)
    <!WRONG_MODIFIER_TARGET!>inner<!> typealias B = @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, TYPE_MISMATCH!>CONST<!>) String<!>
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, const, integerLiteral, primaryConstructor,
propertyDeclaration, stringLiteral, typeAliasDeclaration */
