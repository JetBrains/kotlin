// DO_NOT_REQUIRE_SYMBOL_RESTORATION_K1
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
interface BaseInterface {
    var foo: Int
}

abstract class Base: BaseInterface {
    abstract override var foo: Int
}


// MODULE: main(lib)
// FILE: main.kt
fun usage(b: Base) {
    b.f<caret>oo
}
