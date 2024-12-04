// This file is compiled into each stepping test.

package testUtils

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

external interface ValueDescriptionForSteppingTests {
    var isNull: Boolean?
    var isReferenceType: Boolean?
    var valueDescription: String?
    var typeName: String?
}

external object JSON {
    fun stringify(o: Any?): String
}

/**
 * This function is only called from the debugger
 */
@JsExport
fun makeValueDescriptionForSteppingTests(value: Any?): ValueDescriptionForSteppingTests? {
    val jsTypeName = jsTypeOf(value)
    val displayedTypeName = when (jsTypeName) {
        "undefined" -> return null
        "string", "object", "function" -> if (value == null) jsTypeName else {
            val klass = value::class
            // Fully qualified names are not yet supported in Kotlin/JS reflection
            knownFqNames[klass] ?: klass.simpleName ?: "<anonymous>"
        }
        else -> jsTypeName
    }
    return js("{}").unsafeCast<ValueDescriptionForSteppingTests>().apply {
        isNull = value == null
        isReferenceType = jsTypeName == "object" || jsTypeName == "function"
        valueDescription = when (jsTypeName) {
            "string" -> JSON.stringify(value)
            else -> value.toString()
        }
        typeName = displayedTypeName
    }
}

private val minimalFqNames = mapOf(
    Long::class to "kotlin.Long",
    String::class to "kotlin.String",
    Array::class to "kotlin.Array",
    RuntimeException::class to "kotlin.RuntimeException",
    ArithmeticException::class to "kotlin.ArithmeticException",
)

private val knownFqNames = minimalFqNames + stdlibFqNames

private object EmptyContinuation: Continuation<Any?> {
    override val context: CoroutineContext
        get() = EmptyCoroutineContext

    override fun resumeWith(result: Result<Any?>) {
        result.getOrThrow()
    }
}

@JsExport
fun makeEmptyContinuation(): dynamic = EmptyContinuation
