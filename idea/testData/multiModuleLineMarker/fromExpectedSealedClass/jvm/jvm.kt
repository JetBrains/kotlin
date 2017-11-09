// !CHECK_HIGHLIGHTING

actual sealed class Sealed {

    actual object Sealed1 : Sealed()

    actual class Sealed2 : Sealed() {
        actual val x = 42
        actual fun foo() = ""
    }
}


