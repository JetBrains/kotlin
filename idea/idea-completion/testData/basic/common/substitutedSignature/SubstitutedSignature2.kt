// FIR_COMPARISON
open class C<T> {
    fun foo(t: T): T = t
}

class B : C<String>() {
    fun f() {
        <caret>
    }
}

// EXIST: { itemText: "foo", tailText: "(t: String)", typeText: "String" }
