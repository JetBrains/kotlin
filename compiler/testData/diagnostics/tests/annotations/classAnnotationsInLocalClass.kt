// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE_PARAMETER)
annotation class Anno(val position: String)

open class A<T>

fun foo() {
    val localProp = 1
    @Anno("class $<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>localProp<!>")
    class OriginalClass<@Anno("type param $<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>localProp<!>") T : @Anno("bound $<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>localProp<!>") List<@Anno("nested bound $<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>localProp<!>") Int>> : @Anno("super type $<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>localProp<!>") A<@Anno("nested super type $<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>localProp<!>") List<@Anno("nested nested super type $<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>localProp<!>") Int>>() {
        val prop = 0

        @Anno("class $<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNRESOLVED_REFERENCE!>prop<!>")
        <!NESTED_CLASS_NOT_ALLOWED!>class InnerClass<!><@Anno("type param $<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNRESOLVED_REFERENCE!>prop<!>") T : @Anno("bound $<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNRESOLVED_REFERENCE!>prop<!>") List<@Anno("nested bound $<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNRESOLVED_REFERENCE!>prop<!>") Int>> : @Anno("super type $<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNRESOLVED_REFERENCE!>prop<!>") A<@Anno("nested super type $<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNRESOLVED_REFERENCE!>prop<!>") List<@Anno("nested nested super type $<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNRESOLVED_REFERENCE!>prop<!>") Int>>()
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, integerLiteral, localClass,
localProperty, nestedClass, nullableType, primaryConstructor, propertyDeclaration, stringLiteral, typeConstraint,
typeParameter */
