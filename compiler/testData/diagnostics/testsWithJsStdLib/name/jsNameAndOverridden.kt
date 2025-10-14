// FIR_IDENTICAL
// In K2, the name collision detector is weakened, because the backend started to resolve such collisions.
// K1 was not changed since it's in maintenance mode.

package foo

open class Super {
    fun foo() = 23
}

class Sub : Super() {
    @JsName("foo") fun bar() = 42
}
