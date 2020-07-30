// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE
// !LANGUAGE: -InlineDefaultFunctionalParameters

inline fun unsupported() {

    class A {
        fun a() {
           class AInner {}
        }
    }

    <!LOCAL_OBJECT_NOT_ALLOWED!>object B<!>{
        <!LOCAL_OBJECT_NOT_ALLOWED!>object BInner<!> {}
    }

    fun local() {
        fun localInner() {}
    }
}

inline fun unsupportedDefault(s : ()->Unit = {}) {

}

open class Base {
    open fun foo(a: Int = 1) {}
}

class Derived: Base() {
    inline final override fun foo(a: Int) {

    }
}