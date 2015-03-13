package test

import kotlin.jvm.internal.Intrinsics
import kotlin.jvm.internal.Ref

fun foo(): String {
    // This method call should be resolved to kotlin-runtime.jar
    val r: String = Intrinsics.stringPlus(":", ")")

    // This method call should be resolved to sources
    return Ref.methodWhichDoesNotExistInKotlinRuntime()
}
