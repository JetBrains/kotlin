// RESOLVE_IMPORTED_CALLABLE: bar
// LANGUAGE: +CompanionBlocks +CompanionExtensions
package test

import test.C.bar

annotation class Anno(val s: String)

const val S = "s"

class C {
    companion {
        @Anno("property" + S)
        val bar = "initializer" + S
            @Anno("getter" + S) get() = field + S
    }
}
