// WITH_STDLIB
@Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.PROPERTY)
annotation class Anno(val position: String)

fun foo() {
    class OriginalClass {
        val prop = 0

        @Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"function $prop"<!>)
        fun <@Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"type param $prop"<!>) F : @Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"bound $prop"<!>) List<@Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"nested bound $prop"<!>) List<@Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"nested nested bound $prop"<!>) String>>> @receiver:Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"receiver annotation: $prop"<!>) @Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"receiver type $prop"<!>) Collection<@Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"nested receiver type $prop"<!>) List<@Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"nested nested receiver type $prop"<!>)String>>.explicitType(@Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"parameter annotation $prop"<!>) param: @Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"parameter type $prop"<!>) ListIterator<@Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"nested parameter type $prop"<!>) List<@Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"nested nested parameter type $prop"<!>)String>>): @Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"explicitType return type $prop"<!>) List<@Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"explicitType nested return type $prop"<!>) List<@Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"explicitType nested nested return type $prop"<!>) Int>> = emptyList()

        @Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"property $prop"<!>)
        val <@Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"type param $prop"<!>) F : @Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"bound $prop"<!>) Number> @receiver:Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"receiver annotation: $prop"<!>) @Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"receiver type $prop"<!>) F.explicitType: @Anno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"bound $prop"<!>) Int get() = 1
    }
}
