
// FILE: base.kt
package base

open class Parent {
	open fun run() = "P"
}

inline fun doInline(block: () -> String) = block()

// FILE: impl.kt
package impl

import base.Parent

class Child : Parent() {
	override fun run() = "C"
}

// FILE: test.kt
import impl.Child
import base.doInline

fun box(): String {
	val c = Child()
	return doInline { c.run() }
}

