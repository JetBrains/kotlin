// "Create actual class for module js (JS)" "true"

expect sealed class Sealed

expect object Obj : Sealed

expect class <caret>Klass(x: Int) : Sealed
