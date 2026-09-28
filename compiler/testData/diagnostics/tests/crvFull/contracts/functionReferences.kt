// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts
// KT-85408 returnsResultOf loses ignorable-result analysis for function reference stored in local variable

import kotlin.contracts.*

inline fun <T, R> T.myLet(block: (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        returnsResultOf(block)
    }
    return block(this)
}

class Bar(val s: String)

fun fooS(s: String): String = s

@IgnorableReturnValue
fun ignorableFooS(s: String): String = s

@IgnorableReturnValue
fun ignorableOp(s: String): String = s

fun nonIgnorableOp(s: String): String = s

@IgnorableReturnValue
fun String.extIgnorable(): String = this

fun String.extNonIgnorable(): String = this + "!"

@IgnorableReturnValue
fun <T> idIgnorable(x: T): T = x

fun <T> idNonIgnorable(x: T): T = x

class Container(val value: String) {
    fun process(s: String): String = s + value

    @IgnorableReturnValue
    fun processIgnorable(s: String): String = s + value
}

fun testConstructorReference(s: String?) {
    s?.<!RETURN_VALUE_NOT_USED!>myLet<!>(::Bar)
}

fun testFunctionReference(s: String?) {
    s?.<!RETURN_VALUE_NOT_USED!>myLet<!>(::fooS)
}

fun testIgnorableFunctionReference(s: String?) {
    s?.myLet(::ignorableFooS)
}

fun testBoundFunctionReference(s: String) {
    s.<!RETURN_VALUE_NOT_USED!>myLet<!>(s::plus)
}

fun testBoundMemberFunctionReference(c: Container, s: String?) {
    s?.<!RETURN_VALUE_NOT_USED!>myLet<!>(c::process)
}

fun testBoundMemberFunctionReferenceIgnorable(c: Container, s: String?) {
    s?.myLet(c::processIgnorable)
}

fun testExtensionFunctionReferences(s: String) {
    s.myLet(String::extIgnorable)
    s.<!RETURN_VALUE_NOT_USED!>myLet<!>(String::extNonIgnorable)
}

fun testGenericReferences(s: String) {
    s.myLet(::idIgnorable)
    s.<!RETURN_VALUE_NOT_USED!>myLet<!>(::idNonIgnorable)
}

fun testIgnorableReferences(s: String) {
    val sb = StringBuilder()
    val list = mutableListOf<String>()
    s.myLet(sb::append)
    s.myLet(list::add)
}

fun testNonIgnorableReference(s: String) {
    s.<!RETURN_VALUE_NOT_USED!>myLet<!>(String::length)
}

fun testTopLevelFunctionReferences(s: String) {
    s.<!RETURN_VALUE_NOT_USED!>myLet<!>(::nonIgnorableOp)
    s.myLet(::ignorableOp)
}

/* GENERATED_FIR_TAGS: additiveExpression, callableReference, classDeclaration, contractCallsEffect, contracts,
flexibleType, funWithExtensionReceiver, functionDeclaration, functionalType, inline, javaCallableReference,
javaFunction, lambdaLiteral, nullableType, primaryConstructor, propertyDeclaration, safeCall, stringLiteral,
thisExpression, typeParameter */
