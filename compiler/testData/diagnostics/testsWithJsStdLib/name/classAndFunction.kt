// In K2, the name collision detector is weakened, because the backend started to resolve such collisions.
// K1 was not changed since it's in maintenance mode.

package foo

class <!JS_NAME_CLASH!>A(val x: Int)<!>

<!JS_NAME_CLASH!>fun A()<!> {}
