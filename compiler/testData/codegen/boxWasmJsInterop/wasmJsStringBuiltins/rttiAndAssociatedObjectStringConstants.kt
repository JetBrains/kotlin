// TARGET_BACKEND: WASM
// WITH_STDLIB

@file:OptIn(ExperimentalAssociatedObjects::class)

package wasmjsstrings

import kotlin.reflect.AssociatedObjectKey
import kotlin.reflect.ExperimentalAssociatedObjects
import kotlin.reflect.KClass
import kotlin.reflect.findAssociatedObject

@JsFun("""(s) => typeof s === "string" && !(s instanceof String)""")
external fun jsIsPrimitiveString(s: String): Boolean

@AssociatedObjectKey
@Retention(AnnotationRetention.BINARY)
annotation class Associated(val kClass: KClass<*>)

object AssociatedMarker

@Associated(AssociatedMarker::class)
class Holder {
    class Nested
}

fun box(): String {
    val holderName = Holder::class.qualifiedName ?: return "Fail missing Holder qualifiedName"
    if (holderName != "wasmjsstrings.Holder") return "Fail Holder qualifiedName: <$holderName>"
    if (!jsIsPrimitiveString(holderName)) return "Fail Holder name is not primitive"

    val nestedName = Holder.Nested::class.qualifiedName ?: return "Fail missing Nested qualifiedName"
    if (nestedName != "wasmjsstrings.Holder.Nested") return "Fail Nested qualifiedName: <$nestedName>"
    if (!jsIsPrimitiveString(nestedName)) return "Fail Nested name is not primitive"

    if (Holder::class.findAssociatedObject<Associated>() !== AssociatedMarker) return "Fail associated object"

    val value: Any = 42
    val castMessage = try {
        value as Holder
        return "Fail expected ClassCastException"
    } catch (e: ClassCastException) {
        e.message ?: return "Fail missing cast message"
    }

    if (!castMessage.contains("kotlin.Int")) return "Fail cast source: <$castMessage>"
    if (!castMessage.contains("wasmjsstrings.Holder")) return "Fail cast target: <$castMessage>"
    if (!jsIsPrimitiveString(castMessage)) return "Fail cast message is not primitive"

    return "OK"
}
