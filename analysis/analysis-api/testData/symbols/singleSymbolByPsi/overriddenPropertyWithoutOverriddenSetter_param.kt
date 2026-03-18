// DO_NOT_REQUIRE_SYMBOL_RESTORATION_K1
package pack

interface MyInterface {
    val bar: Int
}

class Impl(override var <caret>bar: Int) : MyInterface