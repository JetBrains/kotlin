open class Base {
    companion object {
        fun foo() {}
    }
}

/**
 * [Child.f<caret>oo]
 */
class Child : Base() { }
