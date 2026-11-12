// DO_NOT_REQUIRE_SYMBOL_RESTORATION
// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION

package test


fun usage() {
    class A {
        fun interface LocalSam {
            fun f()
        }
    }
    A.Loca<caret>lSam { }
}
