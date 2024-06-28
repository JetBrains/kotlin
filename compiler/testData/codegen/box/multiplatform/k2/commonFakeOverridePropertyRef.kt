// LANGUAGE: +MultiPlatformProjects

// MODULE: lib-common
// FILE: common.kt

open class Base {
    open val x = "OK"
}

class Child : Base() {
    fun xGetter() : () -> String = this::x
}

// MODULE: lib()()(lib-common)
// FILE: platform.kt

fun box(): String {
    return Child().xGetter()()
}
