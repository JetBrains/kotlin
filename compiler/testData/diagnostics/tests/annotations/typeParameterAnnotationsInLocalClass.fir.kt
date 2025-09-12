// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
@Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.PROPERTY)
annotation class Anno(val position: String)

fun foo() {
    class OriginalClass {
        val prop = 0

        @Anno("function $<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>prop<!>")
        fun <@Anno("type param $<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>prop<!>") F : @Anno("bound $<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>prop<!>") List<@Anno("nested bound $<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>prop<!>") List<@Anno("nested nested bound $<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>prop<!>") String>>> @receiver:Anno("receiver annotation: $<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>prop<!>") @Anno("receiver type $<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>prop<!>") Collection<@Anno("nested receiver type $<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>prop<!>") List<@Anno("nested nested receiver type $<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>prop<!>")String>>.explicitType(@Anno("parameter annotation $<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>prop<!>") param: @Anno("parameter type $<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>prop<!>") ListIterator<@Anno("nested parameter type $<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>prop<!>") List<@Anno("nested nested parameter type $<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>prop<!>")String>>): @Anno("explicitType return type $<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>prop<!>") List<@Anno("explicitType nested return type $<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>prop<!>") List<@Anno("explicitType nested nested return type $<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>prop<!>") Int>> = emptyList()

        @Anno("property $<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>prop<!>")
        val <@Anno("type param $<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>prop<!>") F : @Anno("bound $<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>prop<!>") Number> @receiver:Anno("receiver annotation: $<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>prop<!>") @Anno("receiver type $<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>prop<!>") F.explicitType: @Anno("bound $<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>prop<!>") Int get() = 1
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, annotationUseSiteTargetReceiver, classDeclaration,
funWithExtensionReceiver, functionDeclaration, getter, integerLiteral, localClass, primaryConstructor,
propertyDeclaration, propertyWithExtensionReceiver, stringLiteral, typeConstraint, typeParameter */
