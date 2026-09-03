// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +LocalTypeAliases
@Target(AnnotationTarget.TYPE, AnnotationTarget.TYPEALIAS, AnnotationTarget.TYPE_PARAMETER)
annotation class Anno(val position: String)

fun foo() {
    class OriginalClass<T> {
        val prop = 0

        @Anno("alias $<!INACCESSIBLE_OUTER_CLASS_RECEIVER, NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>prop<!>")
        typealias NestedTypeAlias <@Anno("type param $<!INACCESSIBLE_OUTER_CLASS_RECEIVER, NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>prop<!>") A : <!BOUND_ON_TYPE_ALIAS_PARAMETER_NOT_ALLOWED!>@Anno("bound $<!INACCESSIBLE_OUTER_CLASS_RECEIVER, NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>prop<!>") Number<!>> = @Anno("type $<!INACCESSIBLE_OUTER_CLASS_RECEIVER, NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>prop<!>") OriginalClass<A>
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, integerLiteral, localClass,
nullableType, primaryConstructor, propertyDeclaration, stringLiteral, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeConstraint, typeParameter */
