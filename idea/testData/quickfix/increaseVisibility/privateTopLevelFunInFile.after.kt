// "Make 'f' internal" "true"
// ACTION: Make 'f' public
// ERROR: Cannot access 'f': it is private in file

package test

fun foo() {
    val x = f()
}
