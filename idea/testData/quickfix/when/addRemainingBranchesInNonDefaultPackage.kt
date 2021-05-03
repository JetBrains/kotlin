// "Add remaining branches" "true"
// ERROR: Unresolved reference: TODO
// ERROR: Unresolved reference: TODO
package test
enum class Color { R, G, B }
fun test(c: Color) = wh<caret>en(c) {
    Color.B -> 0xff
}
/* IGNORE_FIR */
