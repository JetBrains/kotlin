sealed interface MySealedInterface

class OneSealedChild : MySealedInterface
class TwoSealedChild : MySealedInterface

fun testSealed(m: MySealedInterface): String {
    return when (m) {
        is OneSealedChild -> "1"
        is TwoSealedChild -> "2"
    }
}
