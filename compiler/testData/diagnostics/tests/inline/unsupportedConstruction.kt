// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE

inline fun unsupported() {

    <!NOT_YET_SUPPORTED_IN_INLINE!>class A {
        fun a() {
           class AInner {}
        }
    }<!>

    <!NOT_YET_SUPPORTED_IN_INLINE!>object B{
        object BInner {}
    }<!>

    <!NOT_YET_SUPPORTED_IN_INLINE!>fun local() {
        fun localInner() {}
    }<!>
}

inline fun unsupportedDefault(<!NOT_YET_SUPPORTED_IN_INLINE!>s : ()->Unit = {}<!>) {

}

open class Base {
    open fun foo(a: Int = 1) {}
}

class Derived: Base() {
    inline final override fun foo(<!NOT_YET_SUPPORTED_IN_INLINE!>a: Int<!>) {

    }
}