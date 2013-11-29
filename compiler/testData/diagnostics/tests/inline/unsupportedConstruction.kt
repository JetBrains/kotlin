// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE

inline fun unsupported() {

    <!NOT_YET_SUPPORTED_IN_INLINE!>class A {
        fun a() {
           class AInner {}
        }
    }<!>

    <!NOT_YET_SUPPORTED_IN_INLINE!>object B{
        object BInner {}
    }<!>

    val s = <!NOT_YET_SUPPORTED_IN_INLINE!>object {
        fun a() {
            val sInner = object {
                fun aInner() {}
            }
        }
    }<!>

    <!NOT_YET_SUPPORTED_IN_INLINE!>fun local() {
        fun localInner() {}
    }<!>
}