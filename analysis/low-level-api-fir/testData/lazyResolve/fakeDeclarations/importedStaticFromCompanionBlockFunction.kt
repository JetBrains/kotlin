// RESOLVE_IMPORTED_CALLABLE: foo
// LANGUAGE: +CompanionBlocks +CompanionExtensions
package test

import test.C.foo

annotation class Anno(val s: String)

const val S = "s"

class C {
    companion {
        @Anno("function" + S)
        fun foo(@Anno("param" + S) i: Int = 42) = "body" + i
    }
}
