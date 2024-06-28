// TARGET_BACKEND: JVM
// LANGUAGE: +MultiPlatformProjects
// WITH_REFLECT

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

interface I {
    companion object {
        const val OK: String = "OK"
    }
}

fun ok() = I.OK

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: main.kt

import kotlin.reflect.KType

fun getAnnotations(kType: KType) = kType.annotations

fun box() = ok()
