// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +AllowEagerSupertypeAccessibilityChecks

// MODULE: base
// FILE: base.kt

interface SMessage<out T>
interface CMessage<out T>
class Channel<T>
abstract class ServerWebSocketHandler<in S, R, C>

// MODULE: middle(base)
// FILE: middle.kt

interface Transport<Id, T>
interface Document

class Handler<Id, T>(
    private val t: Transport<Id, T>,
) : ServerWebSocketHandler<SMessage<T>, CMessage<T>, Channel<T>>()

// MODULE: use(middle)
// FILE: use.kt

fun handler(t: Transport<String, Document>) {
    <!MISSING_DEPENDENCY_SUPERCLASS("ServerWebSocketHandler; Handler")!>Handler<!>(t)
}

/* GENERATED_FIR_TAGS: classDeclaration, in, interfaceDeclaration, nullableType, out, primaryConstructor,
propertyDeclaration, typeParameter */
