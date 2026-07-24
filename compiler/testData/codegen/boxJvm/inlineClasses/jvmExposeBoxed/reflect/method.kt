// WITH_REFLECT
// TARGET_BACKEND: JVM

@file:OptIn(ExperimentalStdlibApi::class)

package test

import kotlin.reflect.jvm.javaMethod

@JvmInline
value class Id(val value: String) {
    @JvmExposeBoxed
    fun ok(): String = ""
}

fun box(): String {
    val method = Id::ok.javaMethod
    // No change here - the reflection points to the same function as if there is no exposed version.
    // Unlike constructor, it is OK, since the function is not restricted to be called only from
    // inline class generated methods, like 'box-impl'.
    if (method.toString() != "public static final java.lang.String test.Id.ok-impl(java.lang.String)") return method.toString()
    return "OK"
}
