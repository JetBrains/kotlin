// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: MySealedClass.kt
package foo.bar

sealed class MySealedClass

class OneSealedChild : MySealedClass()
class TwoSealedChild : MySealedClass()

// MODULE: main(lib)
// FILE: main.kt

// class: foo/bar/MySealedClass
