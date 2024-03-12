// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: MySealedInterface.kt
package foo.bar

sealed interface MySealedInterface

typealias T1 = MySealedInterface
typealias T2 = T1

class OneSealedChild : MySealedInterface
class TwoSealedChild : T1
class ThreeSealedChild : T2

// MODULE: main(lib)
// FILE: main.kt

// class: foo/bar/MySealedInterface
