// IGNORE_FE10

// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: MySealedInterface.kt
sealed interface MySealedInterface

class OneSealedChild : MySealedInterface
class TwoSealedChild : MySealedInterface

// MODULE: main(lib)
// FILE: main.kt
fun testSealed(m: MySealedInterface): String {
    return when (m) {
        is OneSealedChild -> "1"
    }
}
