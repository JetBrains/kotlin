// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE

inline fun default(s : Int = 10) {

}

inline fun default2(p: Int, s : String = "OK") {

}

open class Base {
    inline final fun foo(a: Int = 1) {}

    inline final fun foo2(a: Int = 1, s: String = "OK") {}
}
