// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// DIAGNOSTICS: -UNUSED_PARAMETER -SUSPENSION_CALL_MUST_BE_USED_AS_RETURN_VALUE
// ISSUE: KT-77836

@kotlin.coroutines.RestrictsSuspension
class RestrictedController {
    suspend fun member() {}
}

suspend fun noReceiver() { }

context(controller: RestrictedController)
suspend fun contextParameter() { }

suspend fun RestrictedController.receiver() { }

suspend fun RestrictedController.test1() {
    member()
    <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>noReceiver<!>()
    contextParameter()
    receiver()
}

context(controller: RestrictedController)
suspend fun test2() {
    controller.member()
    <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>noReceiver<!>()
    contextParameter()
    controller.receiver()
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionDeclarationWithContext,
suspend */
