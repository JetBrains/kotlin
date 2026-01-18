
// FILE: nested.kt
package n

class Outer(val base: Int) {
	class Inner(val x: Int)

	companion object {
		val CONST = "X"
	}
}

fun make(inner: Outer.Inner) = inner.x * 3

// FILE: test.kt
import n.Outer
import n.make

fun box(): String {
	val i = Outer.Inner(4)
	if (make(i) != 12) return "FAIL"
	return Outer.CONST
}

