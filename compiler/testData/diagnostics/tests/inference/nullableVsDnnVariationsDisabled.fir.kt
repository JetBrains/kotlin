// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -InferenceEnhancementsIn21
// ISSUE: KT-61227
// FIR_DUMP

fun <B> goBar(t: B) = Bar<B & Any>(<!ARGUMENT_TYPE_MISMATCH!>t<!>)
fun <BB> goBarNoTypeArguments(t: BB) = <!CANNOT_INFER_PARAMETER_TYPE!>Bar<!>(<!ARGUMENT_TYPE_MISMATCH!>t<!>)

class Bar<BT : Any>(t: BT?)

fun <F> goFoo(t: F) = foo<F & Any>(t)

fun <FT : Any> foo(t: FT?) {}
