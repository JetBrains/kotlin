// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +InferenceEnhancementsIn21
// ISSUE: KT-61227
// FIR_DUMP

fun <B> goBar(t: B) = Bar<B & Any>(t)
fun <BB> goBarNoTypeArguments(t: BB) = Bar(t)

class Bar<BT : Any>(t: BT?)

fun <F> goFoo(t: F) = foo<F & Any>(t)

fun <FT : Any> foo(t: FT?) {}
