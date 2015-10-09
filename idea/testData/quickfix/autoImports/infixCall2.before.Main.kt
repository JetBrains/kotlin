// "Import" "false"
// ERROR: Unresolved reference: foo
// ACTION: Create extension function 'foo'
// ACTION: Create extension property 'foo'
// ACTION: Create local variable 'foo'
// ACTION: Create member function 'foo'
// ACTION: Create member property 'foo'
// ACTION: Create parameter 'foo'
// ACTION: Replace infix call with ordinary call

package h

interface H

fun f(h: H) {
    h <caret>foo h
}