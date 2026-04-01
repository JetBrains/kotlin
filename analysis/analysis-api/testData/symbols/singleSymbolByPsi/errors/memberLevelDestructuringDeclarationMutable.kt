// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION

data class X(val a: Int, val b: Int)

class B {
    v<caret>ar (a, b) = X(1, 2)
}
