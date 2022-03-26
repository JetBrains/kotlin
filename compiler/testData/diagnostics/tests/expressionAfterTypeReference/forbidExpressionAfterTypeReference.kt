// !LANGUAGE: -AllowExpressionAfterTypeReferenceWithoutSpacing
// ISSUE: KT-35811

class A<T>

val reportedProperty: A<String><!EXPRESSION_AFTER_TYPE_REFERENCE_WITHOUT_SPACING_NOT_ALLOWED!>=<!>A()
fun reportedFunction(a: A<String><!EXPRESSION_AFTER_TYPE_REFERENCE_WITHOUT_SPACING_NOT_ALLOWED!>=<!>A()): A<String><!EXPRESSION_AFTER_TYPE_REFERENCE_WITHOUT_SPACING_NOT_ALLOWED!>=<!>a

val unreportedProperty0: A<String> =A()
fun unreportedFunction0(a: A<String> =A()): A<String> =A()

val unreportedProperty1: String=""
fun unreportedFunction1(a: Int=0): Int=a
