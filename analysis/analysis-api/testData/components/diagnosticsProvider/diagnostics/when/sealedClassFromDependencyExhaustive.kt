// IGNORE_FE10
// KT-64503

// MODULE: dependency
// FILE: MySealedClass.kt
sealed class MySealedClass

class OneSealedChild : MySealedClass()
class TwoSealedChild : MySealedClass()

// MODULE: main(dependency)
// FILE: main.kt
fun testSealed(m: MySealedClass): String {
    return when (m) {
        is OneSealedChild -> "1"
        is TwoSealedChild -> "2"
    }
}
