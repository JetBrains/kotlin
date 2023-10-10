// FILE: Base.kt

open class Base {
    companion object {
        fun foo() {}
    }
}

// FILE: main.kt

/**
 * [Base.f<caret>oo]
 */
class Child : Base() { }
