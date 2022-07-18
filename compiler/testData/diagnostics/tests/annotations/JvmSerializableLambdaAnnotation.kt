// LAMBDAS: INDY
// FIR_IDENTICAL
// WITH_STDLIB

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
