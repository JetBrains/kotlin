// IGNORE_FE10
// KT-64503

// MODULE: dependency
// FILE: MySealedInterface.kt
sealed interface MySealedInterface

class OneSealedChild : MySealedInterface
class TwoSealedChild : MySealedInterface

// MODULE: main(dependency)
// FILE: main.kt
fun testSealed(m: MySealedInterface): String {
    return when (m) {
        is OneSealedChild -> "1"
    }
}
