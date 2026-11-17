// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-91430
// WITH_STDLIB
class Hd
@Deprecated("no", level = DeprecationLevel.HIDDEN)
operator fun Hd.iterator(): Iterator<String> = listOf("h").iterator()
fun s9() { for (x in <!OTHER_ERROR_WITH_REASON, OTHER_ERROR_WITH_REASON, OTHER_ERROR_WITH_REASON!>Hd()<!>) <!OTHER_ERROR_WITH_REASON!>println<!>(x) }

/* GENERATED_FIR_TAGS: classDeclaration, forLoop, funWithExtensionReceiver, functionDeclaration, localProperty, operator,
propertyDeclaration, stringLiteral */
