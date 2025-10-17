// RUN_PIPELINE_TILL: FRONTEND
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE_PARAMETER)
annotation class Anno(val position: String)

open class A<T>

fun foo() {
    val localProp = 1
    @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"class $localProp"<!>)
    class OriginalClass<@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"type param $localProp"<!>) T : @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"bound $localProp"<!>) List<@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"nested bound $localProp"<!>) Int>> : @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"super type $localProp"<!>) A<@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"nested super type $localProp"<!>) List<@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"nested nested super type $localProp"<!>) Int>>() {
        val prop = 0

        @Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"class $<!INACCESSIBLE_OUTER_CLASS_RECEIVER!>prop<!>"<!>)
        <!NESTED_CLASS_NOT_ALLOWED!>class InnerClass<!><@Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"type param $<!INACCESSIBLE_OUTER_CLASS_RECEIVER!>prop<!>"<!>) T : @Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"bound $<!INACCESSIBLE_OUTER_CLASS_RECEIVER!>prop<!>"<!>) List<@Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"nested bound $<!INACCESSIBLE_OUTER_CLASS_RECEIVER!>prop<!>"<!>) Int>> : @Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"super type $<!INACCESSIBLE_OUTER_CLASS_RECEIVER!>prop<!>"<!>) A<@Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"nested super type $<!INACCESSIBLE_OUTER_CLASS_RECEIVER!>prop<!>"<!>) List<@Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"nested nested super type $<!INACCESSIBLE_OUTER_CLASS_RECEIVER!>prop<!>"<!>) Int>>()
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, integerLiteral, localClass,
localProperty, nestedClass, nullableType, primaryConstructor, propertyDeclaration, stringLiteral, typeConstraint,
typeParameter */
