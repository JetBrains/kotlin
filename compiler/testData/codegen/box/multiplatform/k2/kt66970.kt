// ISSUE: KT-66970
// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: lib-common
// FILE: lib-common.kt
object O {
    @kotlin.jvm.JvmStatic
    fun ok(): String = "OK"
}

// MODULE: lib()()(lib-common)
// FILE: lib.kt

// MODULE: main(lib)
// FILE: main.kt

fun box() = O.ok()
