// RUN_PIPELINE_TILL: FRONTEND
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE_PARAMETER)
annotation class Anno(val position: String)

open class A<T>

fun foo() {
    val localProp = 1
    @Anno("class $<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>localProp<!>")
    class OriginalClass<@Anno("type param $<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>localProp<!>") T : @Anno("bound $<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>localProp<!>") List<@Anno("nested bound $<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>localProp<!>") Int>> : @Anno("super type $<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>localProp<!>") A<@Anno("nested super type $<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>localProp<!>") List<@Anno("nested nested super type $<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>localProp<!>") Int>>() {
        val prop = 0

        @Anno("class $<!INACCESSIBLE_OUTER_CLASS_RECEIVER, NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>prop<!>")
        <!NESTED_CLASS_NOT_ALLOWED!>class InnerClass<!><@Anno("type param $<!INACCESSIBLE_OUTER_CLASS_RECEIVER, NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>prop<!>") T : @Anno("bound $<!INACCESSIBLE_OUTER_CLASS_RECEIVER, NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>prop<!>") List<@Anno("nested bound $<!INACCESSIBLE_OUTER_CLASS_RECEIVER, NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>prop<!>") Int>> : @Anno("super type $<!INACCESSIBLE_OUTER_CLASS_RECEIVER, NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>prop<!>") A<@Anno("nested super type $<!INACCESSIBLE_OUTER_CLASS_RECEIVER, NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>prop<!>") List<@Anno("nested nested super type $<!INACCESSIBLE_OUTER_CLASS_RECEIVER, NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>prop<!>") Int>>()
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, integerLiteral, localClass,
localProperty, nestedClass, nullableType, primaryConstructor, propertyDeclaration, stringLiteral, typeConstraint,
typeParameter */
