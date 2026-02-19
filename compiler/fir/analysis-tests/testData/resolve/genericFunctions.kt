// RUN_PIPELINE_TILL: BACKEND
interface Any

inline fun <reified T : Any> Any.safeAs(): T? = this as? T

abstract class Summator {
    abstract fun <T> plus(first: T, second: T): T
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, inline, interfaceDeclaration,
nullableType, reified, thisExpression, typeConstraint, typeParameter */
