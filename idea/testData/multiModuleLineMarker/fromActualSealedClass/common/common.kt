// !CHECK_HIGHLIGHTING

expect sealed class Sealed {

    object Sealed1 : Sealed

    class Sealed2 : Sealed {
        val x: Int
        fun foo(): String
    }
}
