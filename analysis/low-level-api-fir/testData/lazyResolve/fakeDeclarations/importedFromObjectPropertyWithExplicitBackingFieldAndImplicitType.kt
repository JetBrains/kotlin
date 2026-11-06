// RESOLVE_IMPORTED_CALLABLE: bar
// LANGUAGE: +ExplicitBackingFields
package test

import test.O.bar

annotation class Anno(val s: String)

const val C = "c"

interface I
class Impl : I

object O {
    @Anno("property" + C)
    val bar
        @Anno("field" + C) field = Impl()
}
