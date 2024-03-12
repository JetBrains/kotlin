package myPack

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class Anno(val number: Int)

val <@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>42.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>prop<!><!>) T> T.prop get() = 22
