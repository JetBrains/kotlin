// RUN_PIPELINE_TILL: FRONTEND
import kotlin.coroutines.coroutineContext

val c = ::<!UNSUPPORTED!>coroutineContext<!>

fun test() {
    c()
}

suspend fun test2() {
    c()
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, propertyDeclaration, suspend */
