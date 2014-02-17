// "Import" "true"
// ERROR: Unresolved reference: foo

package h

trait H

fun f(h: H) {
    h <caret>foo h
}