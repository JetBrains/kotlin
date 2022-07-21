// FIR_IDENTICAL
// !LANGUAGE: +AllowExpressionAfterTypeReferenceWithoutSpacing
// ISSUE: KT-35811

class A<T>

val reportedProperty: A<String>=A()
fun reportedFunction(a: A<String>=A()): A<String>=a

val unreportedProperty0: A<String> =A()
fun unreportedFunction0(a: A<String> =A()): A<String> =A()

val unreportedProperty1: String=""
fun unreportedFunction1(a: Int=0): Int=a