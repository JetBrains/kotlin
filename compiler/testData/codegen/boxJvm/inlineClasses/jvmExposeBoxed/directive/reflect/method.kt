// WITH_REFLECT
// TARGET_BACKEND: JVM
// JVM_EXPOSE_BOXED

package test

import kotlin.reflect.jvm.javaMethod

@JvmInline
value class Id(val value: String) {
    fun ok(): String = ""
}

fun box(): String {
    val method = Id::ok.javaMethod
    if (method.toString() != "public static final java.lang.String test.Id.ok-impl(java.lang.String)") return method.toString()
    return "OK"
}
