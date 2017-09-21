// "Create actual class for platform JS" "true"

expect sealed class <caret>Sealed {
    object Obj : Sealed

    class Klass(x: Int) : Sealed
}