// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE

inline private fun a() {}

inline fun b() {}

inline public fun c() {}

abstract class A {
    inline private fun good1() {}
    inline public final fun good2() {}
    inline protected final fun good3() {}
    inline final fun good4() {}


    inline open protected fun wrong1() {}

    inline open public fun wrong2() {}

    inline open fun wrong3() {}

    inline abstract protected fun wrong4()

    inline abstract public fun wrong5()

    inline abstract fun wrong6()
}


interface B {

    inline private fun good1() {}

    inline fun wrong1() {}

    inline open fun wrong2() {}

    inline open public fun wrong3() {}

    inline open fun wrong4() {}

    inline fun wrong5()

    inline public fun wrong6()

    inline fun wrong7()
}
