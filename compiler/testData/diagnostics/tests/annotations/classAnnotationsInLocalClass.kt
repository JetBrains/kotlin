@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE_PARAMETER)
annotation class Anno(val position: String)

open class A<T>

fun foo() {
    val localProp = 1
    @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"class $localProp"<!>)
    class OriginalClass<@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"type param $localProp"<!>) T : @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"bound $localProp"<!>) List<@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"nested bound $localProp"<!>) Int>> : @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"super type $localProp"<!>) A<@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"nested super type $localProp"<!>) List<@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"nested nested super type $localProp"<!>) Int>>() {
        val prop = 0

        @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"class $<!UNRESOLVED_REFERENCE!>prop<!>"<!>)
        <!NESTED_CLASS_NOT_ALLOWED!>class InnerClass<!><@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"type param $<!UNRESOLVED_REFERENCE!>prop<!>"<!>) T : @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"bound $<!UNRESOLVED_REFERENCE!>prop<!>"<!>) List<@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"nested bound $<!UNRESOLVED_REFERENCE!>prop<!>"<!>) Int>> : @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"super type $<!UNRESOLVED_REFERENCE!>prop<!>"<!>) A<@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"nested super type $<!UNRESOLVED_REFERENCE!>prop<!>"<!>) List<@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"nested nested super type $<!UNRESOLVED_REFERENCE!>prop<!>"<!>) Int>>()
    }
}
