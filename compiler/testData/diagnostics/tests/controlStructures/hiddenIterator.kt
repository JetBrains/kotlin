// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-91430
// LANGUAGE_FEATURE_TOGGLED: ReportMissingErrorsOnForLoopOperatorCalls
// WITH_STDLIB
class Hd
@Deprecated("no", level = DeprecationLevel.HIDDEN)
operator fun Hd.iterator(): Iterator<String> = listOf("h").iterator()
fun s9() { for (x in <!UNRESOLVED_REFERENCE!>Hd<!>()) println(x) }

/* GENERATED_FIR_TAGS: classDeclaration, forLoop, funWithExtensionReceiver, functionDeclaration, localProperty, operator,
propertyDeclaration, stringLiteral */
