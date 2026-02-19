// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
package myPack

annotation class Anno(val number: String)

fun topLevelFun() {
    class LocalClass {
        @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>variableToResolve<!>)
        @field:Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>variableToResolve<!>)
        var variableToResolve = "${42}"
            @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>variableToResolve<!>)
            get() = field + "str"
            @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>variableToResolve<!>)
            set(@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>variableToResolve<!>) value) = Unit
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, annotationDeclaration, annotationUseSiteTargetField, classDeclaration,
functionDeclaration, getter, integerLiteral, localClass, primaryConstructor, propertyDeclaration, setter, stringLiteral */
