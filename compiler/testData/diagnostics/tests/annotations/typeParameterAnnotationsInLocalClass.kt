// WITH_STDLIB
@Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.PROPERTY)
annotation class Anno(val position: String)

fun foo() {
    class OriginalClass {
        val prop = 0

        @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"function $prop"<!>)
        fun <@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"type param $prop"<!>) F : @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"bound $prop"<!>) List<@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"nested bound $prop"<!>) List<@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"nested nested bound $prop"<!>) String>>> @receiver:Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"receiver annotation: $prop"<!>) <!REPEATED_ANNOTATION!>@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"receiver type $prop"<!>)<!> Collection<@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"nested receiver type $prop"<!>) List<@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"nested nested receiver type $prop"<!>)String>>.explicitType(@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"parameter annotation $prop"<!>) param: @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"parameter type $prop"<!>) ListIterator<@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"nested parameter type $prop"<!>) List<@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"nested nested parameter type $prop"<!>)String>>): @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"explicitType return type $prop"<!>) List<@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"explicitType nested return type $prop"<!>) List<@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"explicitType nested nested return type $prop"<!>) Int>> = emptyList()

        @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"property $prop"<!>)
        val <@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"type param $prop"<!>) F : @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"bound $prop"<!>) Number> @receiver:Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"receiver annotation: $prop"<!>) <!REPEATED_ANNOTATION!>@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"receiver type $prop"<!>)<!> F.explicitType: @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"bound $prop"<!>) Int get() = 1
    }
}
