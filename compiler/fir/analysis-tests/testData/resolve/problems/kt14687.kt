// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-14687
// WITH_STDLIB

// KT-14687: Missing "Incompatible types" error for `is` checks when one of types is open

open class A
open class B : A()
class C : B()
class D
class E<T>

fun f6(p: E<Int>) {
    println(when (p) {
        <!IMPOSSIBLE_IS_CHECK_ERROR!>is A<!> -> "A" // no error (BUG: should report INCOMPATIBLE_TYPES)
        <!IMPOSSIBLE_IS_CHECK_ERROR!>is B<!> -> "B" // no error (BUG: should report INCOMPATIBLE_TYPES)
        <!IMPOSSIBLE_IS_CHECK_ERROR!>is C<!> -> "C"
        <!IMPOSSIBLE_IS_CHECK_ERROR!>is D<!> -> "D"
        else -> "else"
    })
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, isExpression, nullableType, smartcast, stringLiteral,
typeParameter, whenExpression, whenWithSubject */