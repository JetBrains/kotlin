open class Bar {
    companion object {
        class FromBarCompanion {
            fun foo() = 42
        }
    }
}

class Foo : Bar() {
    val a = <selection>Companion.FromBarCompanion::foo</selection>
}