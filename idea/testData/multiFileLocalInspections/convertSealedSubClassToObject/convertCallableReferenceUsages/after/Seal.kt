package seal

sealed class Sealed

object SubSealed : Sealed() {
    class Nested

    fun internalFunction() {}
}