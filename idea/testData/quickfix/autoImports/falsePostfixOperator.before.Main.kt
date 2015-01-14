// "Import" "false"
// ACTION: Create function 'inc'
// ACTION: Create local variable '++'
// ACTION: Create parameter '++'
// ACTION: Replace overloaded operator with function call
// ERROR: Unresolved reference: ++

package h

trait H

[suppress("UNUSED_CHANGED_VALUE")]
fun f(h: H?) {
    var h1 = h
    h1<caret>++
}