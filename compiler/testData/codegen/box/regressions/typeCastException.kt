// TARGET_BACKEND: JVM

// WITH_RUNTIME

import java.util.ArrayList

// KT-2823 TypeCastException has no message
// KT-5121 Better error message in on casting null to non-null type

fun box(): String {
    try {
        val a: Any? = null
        a as Array<String>
    }
    catch (e: TypeCastException) {
        if (e.message != "null cannot be cast to non-null type kotlin.Array<kotlin.String>") {
            return "Fail 1: $e"
        }
    }

    try {
        val x: String? = null
        x as String
    }
    catch (e: TypeCastException) {
        if (e.message != "null cannot be cast to non-null type kotlin.String") {
            return "Fail 2: $e"
        }
    }

    return "OK"
}
