// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-86467
import kotlin.coroutines.RestrictsSuspension

@RestrictsSuspension
interface Scope {
    suspend fun member()
}

suspend fun bar() {}

suspend fun <T : Scope?> (T & Any).foo() {
    bar()
}

class Box<T>(val value: T)

interface I

suspend fun bar(tl: ThreadLocal<Scope>, box: Box<out Scope>, scope: Scope?) {
    tl.get().member()
    box.value.member()

    if (scope is I) {
        scope.member()
    }
}

/* GENERATED_FIR_TAGS: dnnType, flexibleType, funWithExtensionReceiver, functionDeclaration, interfaceDeclaration,
nullableType, outProjection, suspend, typeConstraint, typeParameter */
