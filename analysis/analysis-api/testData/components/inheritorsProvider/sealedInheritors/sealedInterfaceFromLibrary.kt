// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: MySealedInterface.kt
package foo.bar

sealed interface MySealedInterface

class OneSealedChild : MySealedInterface
class TwoSealedChild : MySealedInterface

// MODULE: main(lib)
// FILE: main.kt

// class: foo/bar/MySealedInterface
