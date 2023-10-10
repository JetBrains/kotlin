// UNRESOLVED_REFERENCE

open class Base {
    companion object {
        fun foo() {}
    }
}

/**
 * [f<caret>oo]
 */
class Child : Base() { }
