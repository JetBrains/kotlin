// In K2, the name collision detector is weakened, because the backend started to resolve such collisions.
// K1 was not changed since it's in maintenance mode.

package foo

<!JS_NAME_CLASH!>fun bar()<!> = 23

<!JS_NAME_CLASH!>val bar<!> = 32
