object Obj {
    class NestedInObject {
        companion object {
            val inCompanion = 0
        }
    }
}

typealias TA = Obj.NestedInObject

val usage = TA.<caret>

// EXIST: inCompanion