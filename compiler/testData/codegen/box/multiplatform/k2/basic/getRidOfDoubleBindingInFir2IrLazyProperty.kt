// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM
// WITH_REFLECT

// MODULE: common
// FILE: common.kt

interface I {
    companion object {
        const val OK: String = "OK"
    }
}

fun ok() = I.OK

// MODULE: jvm()()(common)
// FILE: main.kt

import kotlin.reflect.KType

fun getAnnotations(kType: KType) = kType.annotations

fun box() = ok()
