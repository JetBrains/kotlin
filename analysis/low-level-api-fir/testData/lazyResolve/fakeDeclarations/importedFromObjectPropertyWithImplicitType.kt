// RESOLVE_IMPORTED_CALLABLE: bar
package test

import test.O.bar

annotation class Anno(val s: String)

const val C = "c"

object O {
    @Anno("property" + C)
    val bar = "initializer" + C
        @Anno("getter" + C) get() = field + C
}
