// RUN_PIPELINE_TILL: BACKEND
// KT-174 Nullability info for extension function receivers
interface Tree {}

fun Any?.TreeValue() : Tree {
  if (this is Tree) return <!DEBUG_INFO_SMARTCAST!>this<!>
    throw Exception()
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, ifExpression, interfaceDeclaration, isExpression,
nullableType, smartcast, thisExpression */
