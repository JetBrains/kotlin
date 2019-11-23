// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.jvm.reflect

val x = { OK: String -> }

fun box(): String {
    return x.reflect()?.parameters?.singleOrNull()?.name ?: "null"
}
