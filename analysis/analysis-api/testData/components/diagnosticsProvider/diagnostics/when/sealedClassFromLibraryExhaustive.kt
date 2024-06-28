// IGNORE_FE10

// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: MySealedClass.kt
sealed class MySealedClass

class OneSealedChild : MySealedClass()
class TwoSealedChild : MySealedClass()

// MODULE: main(lib)
// FILE: main.kt
fun testSealed(m: MySealedClass): String {
    return when (m) {
        is OneSealedChild -> "1"
        is TwoSealedChild -> "2"
    }
}
