// "Import" "true"
// ERROR: Unresolved reference: *=

trait H

fun f(h: H) {
    h <caret>*= 3
}
