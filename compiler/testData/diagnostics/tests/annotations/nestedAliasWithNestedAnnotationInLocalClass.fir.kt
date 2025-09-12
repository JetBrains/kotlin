// RUN_PIPELINE_TILL: FRONTEND
@Target(AnnotationTarget.TYPE, AnnotationTarget.TYPEALIAS, AnnotationTarget.TYPE_PARAMETER)
annotation class Anno(val position: String)

fun foo() {
    class OriginalClass<T> {
        val prop = 0

        <!UNSUPPORTED_FEATURE!>@Anno("alias $<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNRESOLVED_REFERENCE!>prop<!>")
        typealias NestedTypeAlias <@Anno("type param $<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNRESOLVED_REFERENCE!>prop<!>") A : <!BOUND_ON_TYPE_ALIAS_PARAMETER_NOT_ALLOWED!>@Anno("bound $<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNRESOLVED_REFERENCE!>prop<!>") Number<!>> = @Anno("type $<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNRESOLVED_REFERENCE!>prop<!>") OriginalClass<A><!>
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, integerLiteral, localClass,
nullableType, primaryConstructor, propertyDeclaration, stringLiteral, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeConstraint, typeParameter */
