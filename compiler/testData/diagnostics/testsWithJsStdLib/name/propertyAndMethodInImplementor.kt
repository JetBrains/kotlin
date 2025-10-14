// FIR_IDENTICAL
// In K2, the name collision detector is weakened, because the backend started to resolve such collisions.
// K1 was not changed since it's in maintenance mode.

package foo

interface I {
    fun foo() = 23
}

class Sub : I {
    var foo = 42
}
