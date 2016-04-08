// "Make prop internal" "true"
// ERROR: Cannot access 'prop': it is 'private' in file

package test

fun foo() {
    val x = <caret>prop
}
