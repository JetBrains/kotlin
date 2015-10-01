// "Make <set-prop> internal" "true"
// ERROR: Cannot assign to 'prop': the setter is 'private' in file

package test

fun foo() {
    <caret>prop = 20
}
