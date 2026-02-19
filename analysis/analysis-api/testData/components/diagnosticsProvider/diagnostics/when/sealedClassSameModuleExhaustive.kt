sealed class MySealedClass

class OneSealedChild : MySealedClass()
class TwoSealedChild : MySealedClass()

fun testSealed(m: MySealedClass): String {
    return when (m) {
        is OneSealedChild -> "1"
        is TwoSealedChild -> "2"
    }
}
