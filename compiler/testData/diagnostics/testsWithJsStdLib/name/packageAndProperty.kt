// FILE: foo.kt

package foo

<!JS_NAME_CLASH!>val bar<!> = 23

// FILE: foobar.kt

package foo.bar

val x = 42
