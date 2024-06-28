// LANGUAGE: +MultiPlatformProjects

// MODULE: lib-common
// FILE: common.kt


abstract expect class Base
expect class Child : Base


// MODULE: lib()()(lib-common)
// FILE: platform.kt

actual abstract class Base() {
    abstract fun foo(): Any
}

actual class Child: Base() {
    override fun foo(): String = "OK"
}

fun box(): String {
    return Child().foo()
}
