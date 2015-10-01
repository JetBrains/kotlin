// "Make prop internal" "true"
// ERROR: Cannot access 'prop': it is 'private' in file

package test

fun foo() {
    <caret>prop = 20
}
