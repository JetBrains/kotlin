trait X<T>

fun<T> X<T>.contains(t: T): Boolean

trait A {
    fun<T> createX(t: T): X<T>

    fun foo(s: String) {
        if (s in <caret>)
    }
}

// EXIST: { lookupString:"createX", itemText: "createX", tailText: "(t: T)", typeText:"X<T>" }
