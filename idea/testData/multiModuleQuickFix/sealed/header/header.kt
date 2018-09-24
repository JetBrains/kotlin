// "Create actual class for module testModule_JS (JS)" "true"

expect sealed class <caret>Sealed {
    object Obj : Sealed

    class Klass(x: Int) : Sealed
}