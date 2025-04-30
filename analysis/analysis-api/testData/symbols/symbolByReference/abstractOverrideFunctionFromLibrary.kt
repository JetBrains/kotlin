// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
interface BaseInterface {
    fun foo(): Int
}

abstract class Base: BaseInterface {
    abstract override fun foo(): Int
}


// MODULE: main(lib)
// FILE: main.kt
fun usage(b: Base) {
    b.f<caret>oo()
}
