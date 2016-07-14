// "Import" "false"
// ACTION: Create extension function 'H?.inc'
// ACTION: Create member function 'H.inc'
// ACTION: Replace overloaded operator with function call
// ERROR: Unresolved reference: ++

package h

interface H

@Suppress("UNUSED_CHANGED_VALUE")
fun f(h: H?) {
    var h1 = h
    h1<caret>++
}