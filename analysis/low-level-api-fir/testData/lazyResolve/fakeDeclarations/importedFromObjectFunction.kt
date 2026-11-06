// RESOLVE_IMPORTED_CALLABLE: foo
package test

import test.O.foo

annotation class Anno(val s: String)

const val C = "c"

object O {
    @Anno("function" + C)
    fun <@Anno("typeParameter" + C) T> foo(@Anno("param" + C) t: T, i: Int = 42): String = "body" + t + i
}
