// "Add explicit import" "true"

open class Bar {
    companion object {
        class FromBarCompanion
    }
}

class Foo : Bar() {
    val a: <caret>FromBarCompanion? = null
}