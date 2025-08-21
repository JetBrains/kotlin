// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-78324

interface A<T>
interface B : A<String>
typealias C<F> = A<F>

private fun testClass(): Any = object : <!INCONSISTENT_TYPE_PARAMETER_VALUES!>A<Int>, B<!> {}
private fun testTypeAlias(): Any = object : <!INCONSISTENT_TYPE_PARAMETER_VALUES!>C<Int>, B<!> {}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, functionDeclaration, interfaceDeclaration, nullableType, typeParameter */
