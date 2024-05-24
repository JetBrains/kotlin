open class Base {
    companion object {
        fun foo() {}
    }
}

/**
 * [Base.f<caret>oo]
 */
class Child : Base() { }
