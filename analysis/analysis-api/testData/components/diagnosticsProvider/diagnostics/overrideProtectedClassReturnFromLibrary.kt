// IGNORE_FE10
// KT-64503

// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: TestExposeTypeLib.kt
abstract class TestExposeTypeLib {
    protected abstract fun returnProtectedClass(): ProtectedClass?
    protected class ProtectedClass
}

// MODULE: main(lib)
// FILE: usage.kt
class Usage : TestExposeTypeLib() {
    override fun returnProtectedClass(): ProtectedClass? = null
}
