// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_K1: ANY
// IGNORE_BACKEND: WASM
// MODULE: common
// FILE: common.kt

open class Base {
    open operator fun plus(b: Base) = Base()
}

expect open class Derived constructor() : Base() {
}

// MODULE: main()()(common)
// FILE: main.kt

actual open class Derived : Base() {
    // Any.equals is is external on Native but because it's not expect, it's ok to override without external
    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode() = 1
    override fun toString() = "Derived"

    // no operator modifier is legal because Base.plus is not expect
    override fun plus(b: Base): Base = Derived()
}

fun box() = "OK"
