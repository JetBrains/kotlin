// RUN_PIPELINE_TILL: FRONTEND
// LAMBDAS: INDY
// WITH_STDLIB
// LANGUAGE: +ForbidJvmSerializableLambdaOnInlinedFunctionLiterals

import kotlin.jvm.JvmSerializableLambda

fun foo() = fun () {}

val good1 = @JvmSerializableLambda {}
val good2 = @JvmSerializableLambda fun () {}
val good3 = @JvmSerializableLambda fun Any.() {}
val good4 = listOf(@JvmSerializableLambda {})[0]

val bad1 = <!WRONG_ANNOTATION_TARGET!>@JvmSerializableLambda<!> 1
val bad2 = <!WRONG_ANNOTATION_TARGET!>@JvmSerializableLambda<!> object {}
val bad3 = <!WRONG_ANNOTATION_TARGET!>@JvmSerializableLambda<!> ::foo
val bad4 = listOf(<!WRONG_ANNOTATION_TARGET!>@JvmSerializableLambda<!> 1)[0]
inline fun inlineArg(x: () -> Unit) = Unit
inline fun crossInlineArg(crossinline x: () -> Unit) = Unit
<!NOTHING_TO_INLINE!>inline<!> fun noInlineArg(noinline x: () -> Unit) = Unit
fun bad5() {
    inlineArg @JvmSerializableLambda {  }
    crossInlineArg @JvmSerializableLambda {  }
    noInlineArg @JvmSerializableLambda {  }
}
fun bad6() {
    inlineArg (@JvmSerializableLambda fun () {})
    crossInlineArg (@JvmSerializableLambda fun () {})
    noInlineArg (@JvmSerializableLambda fun () {})
}
