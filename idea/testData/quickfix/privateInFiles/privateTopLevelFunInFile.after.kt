// "Make f internal" "true"
// ERROR: Cannot access 'f': it is 'private' in file

package test

fun foo() {
    val x = f()
}
