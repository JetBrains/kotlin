import seal.*

val sealedOutsideClass = SubSealed

class KotlinSealedTest {
    val sealedInsideClass = SubSealed

    fun testSeal() {
        val sealedInsideMethod = SubSealed

        SubSealed
    }
}