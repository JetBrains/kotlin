// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +LocalTypeAliases
@Target(AnnotationTarget.TYPE, AnnotationTarget.TYPEALIAS, AnnotationTarget.TYPE_PARAMETER)
annotation class Anno(val position: String)

fun foo() {
    class OriginalClass<T> {
        val prop = 0

        @Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"alias $<!INACCESSIBLE_OUTER_CLASS_RECEIVER!>prop<!>"<!>)
        typealias NestedTypeAlias <@Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"type param $<!INACCESSIBLE_OUTER_CLASS_RECEIVER!>prop<!>"<!>) A : <!BOUND_ON_TYPE_ALIAS_PARAMETER_NOT_ALLOWED!>@Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"bound $<!INACCESSIBLE_OUTER_CLASS_RECEIVER!>prop<!>"<!>) Number<!>> = @Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"type $<!INACCESSIBLE_OUTER_CLASS_RECEIVER!>prop<!>"<!>) OriginalClass<A>
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, integerLiteral, localClass,
nullableType, primaryConstructor, propertyDeclaration, stringLiteral, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeConstraint, typeParameter */
