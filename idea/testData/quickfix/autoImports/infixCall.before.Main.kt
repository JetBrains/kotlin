// "Import" "true"
// ERROR: Unresolved reference: foo

trait H

fun f(h: H) {
    h <caret>foo h
}