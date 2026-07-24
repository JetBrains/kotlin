// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-86467
import kotlin.coroutines.RestrictsSuspension

@RestrictsSuspension
interface Scope {
    suspend fun member()
}

suspend fun bar() {}

suspend fun <T : Scope?> (T & Any).foo() {
    <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>bar<!>()
}

class Box<T>(val value: T)

interface I

suspend fun bar(tl: ThreadLocal<Scope>, box: Box<out Scope>, scope: Scope?) {
    tl.get().<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>member<!>()
    box.value.<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>member<!>()

    if (scope is I) {
        scope.<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>member<!>()
    }
}

/* GENERATED_FIR_TAGS: dnnType, flexibleType, funWithExtensionReceiver, functionDeclaration, interfaceDeclaration,
nullableType, outProjection, suspend, typeConstraint, typeParameter */
