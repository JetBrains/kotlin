// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocks +CompanionExtensions
// RENDER_DIAGNOSTIC_ARGUMENTS
// RENDER_DIAGNOSTICS_FULL_TEXT
// ISSUE: KT-91409

fun test() {
    <!CANNOT_INFER_PARAMETER_TYPE("T"), UNRESOLVED_REFERENCE_WRONG_RECEIVER("companion fun <T> Function1.invoke(): (T) -> Unit; () -> R (of interface Function0<out R>)")!>Function0<!>()
}

companion operator fun <T> Function1.invoke(): (T) -> Unit = {}

companion fun Function0.a02() {}
fun t02() { Function1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER("companion fun Function0.a02(): Unit; (P1 (of interface Function1<in P1, out R>)) -> R (of interface Function1<in P1, out R>)")!>a02<!>() }

companion fun Function22.a04() {}
fun t04() { Function1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER("companion fun Function22.a04(): Unit; (P1 (of interface Function1<in P1, out R>)) -> R (of interface Function1<in P1, out R>)")!>a04<!>() }

companion fun kotlin.coroutines.SuspendFunction1.a06() {}
fun t06() { Function0.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER("companion fun SuspendFunction1.a06(): Unit; () -> R (of interface Function0<out R>)")!>a06<!>() }

companion fun <!COMPANION_EXTENSION_NULLABLE_RECEIVER!>Function1?<!>.a38() {}
fun t38() { Function0.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER("companion fun Function1?.a38(): Unit; () -> R (of interface Function0<out R>)")!>a38<!>() }

typealias F17<A, B> = Function1<A, B>
companion fun F17.a17() {}
fun t17() { Function0.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER("companion fun Function1.a17(): Unit; () -> R (of interface Function0<out R>)")!>a17<!>() }

companion val Function1.p16: Int get() = 1
fun t16() { Function0.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER("companion val Function1.p16: Int; () -> R (of interface Function0<out R>)")!>p16<!> }

companion fun Function1.a11() {}
fun t11() { Function1.a11(<!TOO_MANY_ARGUMENTS("companion fun Function1.a11(): Unit")!>1<!>) }

companion fun Function1.a15(x: Int) {}
companion fun Function1.a15(x: String) {}
fun t15() { Function1.<!NONE_APPLICABLE("companion fun Function1.a15(x: Int): Unit:  Argument type mismatch: actual type is 'Double', but 'Int' was expected.companion fun Function1.a15(x: String): Unit:  Argument type mismatch: actual type is 'Double', but 'String' was expected.")!>a15<!>(1.0) }

companion fun Function1.a49(vararg x: Int) {}
companion fun Function1.a49(vararg x: Long) {}
fun t49() { Function1.<!OVERLOAD_RESOLUTION_AMBIGUITY("companion fun Function1.a49(vararg x: Int): Unitcompanion fun Function1.a49(vararg x: Long): Unit")!>a49<!>() }

companion suspend fun Function1.a28() {}
fun t28() { Function1.<!ILLEGAL_SUSPEND_FUNCTION_CALL("companion suspend fun Function1.a28(): Unit")!>a28<!>() }

@Deprecated("x", level = DeprecationLevel.ERROR)
companion fun Function1.a30() {}
fun t30() { Function1.<!DEPRECATION_ERROR("companion fun Function1.a30(): Unit;  x.")!>a30<!>() }

companion fun Function1.a25() {}
companion fun Function2.a25() {}
fun t25() { Function0.<!NONE_APPLICABLE("companion fun Function1.a25(): Unit:  Candidate 'companion fun Function1.a25(): Unit' is inapplicable because of a receiver type mismatch. Actual receiver type is '() -> R (of interface Function0<out R>)'.companion fun Function2.a25(): Unit:  Candidate 'companion fun Function2.a25(): Unit' is inapplicable because of a receiver type mismatch. Actual receiver type is '() -> R (of interface Function0<out R>)'.")!>a25<!>() }

@Deprecated("x")
companion fun Function1.a12(): String = "ok"
fun m12() { Function1.<!DEPRECATION("companion fun Function1.a12(): String;  x.")!>a12<!>() }

@Deprecated("x")
companion val Function1.p31: Int get() = 1
fun m31() { Function1.<!DEPRECATION("companion val Function1.p31: Int;  x.")!>p31<!> }

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionalType, getter, integerLiteral,
lambdaLiteral, nullableType, operator, propertyDeclaration, propertyWithExtensionReceiver, stringLiteral, suspend,
typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter, vararg */
