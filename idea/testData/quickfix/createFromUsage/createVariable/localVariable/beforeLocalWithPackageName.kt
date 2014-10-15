// "Create local variable 'foo'" "true"
// ACTION: Create parameter 'foo'
// ERROR: Variable 'foo' must be initialized

package foo

fun test(): Int {
    return <caret>foo
}