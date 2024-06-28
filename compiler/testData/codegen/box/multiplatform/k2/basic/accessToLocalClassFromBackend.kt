// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt

val LocalClass = object {
    override fun toString() = "OK"
}

fun ok() = LocalClass.toString()

// MODULE: platform()()(common)
// FILE: platform.kt

fun box() = ok()
