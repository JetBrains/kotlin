// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +LocalTypeAliases
@Target(AnnotationTarget.TYPE, AnnotationTarget.TYPEALIAS, AnnotationTarget.TYPE_PARAMETER)
annotation class Anno(val position: String)

fun foo() {
    class OriginalClass<T> {
        val prop = 0

        @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"alias $<!UNRESOLVED_REFERENCE!>prop<!>"<!>)
        typealias NestedTypeAlias <@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"type param $<!UNRESOLVED_REFERENCE!>prop<!>"<!>) A : <!BOUND_ON_TYPE_ALIAS_PARAMETER_NOT_ALLOWED!>@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"bound $<!UNRESOLVED_REFERENCE!>prop<!>"<!>) Number<!>> = @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"type $<!UNRESOLVED_REFERENCE!>prop<!>"<!>) OriginalClass<A>
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, integerLiteral, localClass,
nullableType, primaryConstructor, propertyDeclaration, stringLiteral, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeConstraint, typeParameter */
