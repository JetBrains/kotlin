package test

sealed class Sealed {
    class Nested: Sealed()
    object Top: Sealed()
}
