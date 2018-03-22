// "Create actual class for module js (JS)" "true"

expect sealed class <caret>Sealed {
    object Obj : Sealed

    class Klass(x: Int) : Sealed
}