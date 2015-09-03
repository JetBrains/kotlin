open class A<T>

interface I

class B : A<String>(), I {
    fun foo() {
        super<<caret>
    }
}

// EXIST: { itemText: "A", tailText: " (<root>)" }
