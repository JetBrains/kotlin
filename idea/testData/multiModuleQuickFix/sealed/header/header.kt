// "Create header class implementation for platform JS" "true"

header sealed class <caret>Sealed {
    object Obj : Sealed()

    class Klass(val x: Int) : Sealed()
}