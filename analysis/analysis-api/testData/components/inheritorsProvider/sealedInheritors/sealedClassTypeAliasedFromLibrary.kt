// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: MySealedClass.kt
sealed class MySealedClass

typealias T1 = MySealedClass
typealias T2 = T1

class OneSealedChild : MySealedClass()
class TwoSealedChild : T1()
class ThreeSealedChild : T2()

// MODULE: main(lib)
// FILE: main.kt

// class: MySealedClass
