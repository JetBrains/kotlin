// "Replace with 'X<Int>'" "true"

package ppp

class X<T>

@Deprecated("Will be dropped", replaceWith = ReplaceWith("X<Int>", "ppp.X"))
typealias IntX = X<Int>

fun foo(ix: <caret>IntX) {}