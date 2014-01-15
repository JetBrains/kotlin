// "Import" "true"
// ERROR: Unresolved reference: -

package h

trait H

fun f(h: H?) {
    <caret>-h
}