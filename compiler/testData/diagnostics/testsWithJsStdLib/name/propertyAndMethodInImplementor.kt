// In K2, the name collision detector is weakened, because the backend started to resolve such collisions.
// K1 was not changed since it's in maintenance mode.

package foo

interface I {
    <!JS_NAME_CLASH!>fun foo()<!> = 23
}

class Sub : I {
    <!JS_NAME_CLASH!>var foo<!> = 42
}
