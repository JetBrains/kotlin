// "Import" "true"
// ERROR: Unresolved reference: ++

trait H

fun f(h: H?) {
    var h1 = h
    h1<caret>++
}