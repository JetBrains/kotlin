// MODULE: lib
// FILE: A.kt
abstract class Base {
    abstract var x: String
        internal set
}

class Derived: Base() {
    override var x: String = "Z"
}

// MODULE: main()(lib)
// FILE: B.kt
fun box(): String {
    val d = Derived()
    d.x = "OK"
    return d.x
}
