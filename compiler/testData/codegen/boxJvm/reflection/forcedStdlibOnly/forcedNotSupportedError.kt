// TARGET_BACKEND: JVM
// WITH_REFLECT
// FORCE_STDLIB_ONLY_REFLECTION

import kotlin.reflect.full.createType

fun box(): String {
    try {
        Int::class.createType()
        return "Fail: expected KotlinReflectionNotSupportedError"
    } catch (e: KotlinReflectionNotSupportedError) {
        if (e.message != "Full Kotlin reflection implementation cannot be applied for the references from " +
            "the code compiled with '-Xforce-stdlib-only-reflection' option")
            return "Fail: wrong error message: ${e.message}"
    }
    return "OK"
}