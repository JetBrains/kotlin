package test

import kotlin.jvm.internal.KObject
import kotlin.jvm.internal.Intrinsics

fun foo(): String {
    // This method call should be resolved to kotlin-runtime.jar
    val r: String = Intrinsics.stringPlus(":", ")")

    // This method call should be resolved to sources
    return KObject.methodWhichDoesNotExistInKotlinRuntime()
}
