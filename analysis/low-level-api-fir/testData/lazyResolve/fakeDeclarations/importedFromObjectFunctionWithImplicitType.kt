// RESOLVE_IMPORTED_CALLABLE: foo
package test

import test.O.foo

annotation class Anno(val s: String)

const val C = "c"

object O {
    @Anno("function" + C)
    fun foo(@Anno("param" + C) i: Int = 42) = "body" + i
}
