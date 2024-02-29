// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: MySealedClass.kt
sealed class MySealedClass

class OneSealedChild : MySealedClass()
class TwoSealedChild : MySealedClass()

// MODULE: main(lib)
// FILE: main.kt

// class: MySealedClass
