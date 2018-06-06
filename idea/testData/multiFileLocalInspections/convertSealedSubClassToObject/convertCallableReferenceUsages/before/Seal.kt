package seal

sealed class Sealed

<caret>class SubSealed : Sealed() {
    class Nested

    fun internalFunction() {}
}