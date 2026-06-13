// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// DIAGNOSTICS: -UNUSED_PARAMETER -SUSPENSION_CALL_MUST_BE_USED_AS_RETURN_VALUE
// ISSUE: KT-77836

import kotlin.coroutines.RestrictsSuspension

@RestrictsSuspension
interface Scope

suspend fun Scope.foo1(
    a1: suspend Scope.() -> Unit,
    a2: suspend () -> Unit,
    a3: suspend String.() -> Unit,
    a4: suspend context(Scope) () -> Unit
) {
    a1()
    this.a1()
    <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>a2<!>()
    "".<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>a3<!>()
    a4()
}

context(scope: Scope)
suspend fun foo2(
    a1: suspend Scope.() -> Unit,
    a2: suspend () -> Unit,
    a3: suspend String.() -> Unit,
    a4: suspend context(Scope) () -> Unit
) {
    scope.a1()
    <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>a2<!>()
    "".<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>a3<!>()
    a4()
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionDeclarationWithContext, functionalType,
interfaceDeclaration, stringLiteral, suspend, thisExpression, typeWithContext, typeWithExtension */
