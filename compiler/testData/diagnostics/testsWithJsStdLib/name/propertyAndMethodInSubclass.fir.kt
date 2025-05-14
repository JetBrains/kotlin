// In K2, the name collision detector is weakened, because the backend started to resolve such collisions.
// K1 was not changed since it's in maintenance mode.

package foo

open class Super {
    val foo = 23
}

class Sub : Super() {
    fun foo() = 42
}
