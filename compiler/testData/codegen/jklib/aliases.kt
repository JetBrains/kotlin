
// FILE: klib.kt
package defs

inline class Wrapper(val s: String)

typealias StrList = List<String>

fun combine(w: Wrapper, list: StrList): String =
	w.s + list.joinToString()

// FILE: test.kt
import defs.Wrapper
import defs.StrList
import defs.combine

fun box(): String {
	val w = Wrapper("X")
	val l: StrList = listOf("Y", "Z")
	return combine(w, l)
}

