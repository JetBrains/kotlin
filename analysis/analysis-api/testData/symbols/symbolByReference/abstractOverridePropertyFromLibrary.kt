// DO_NOT_CHECK_SYMBOL_RESTORE_K1
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
