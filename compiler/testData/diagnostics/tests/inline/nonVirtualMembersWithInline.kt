// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE

inline private fun a() {}

inline fun b() {}

inline public fun c() {}

abstract class A {
    inline private fun good1() {}
    inline public final fun good2() {}
    inline protected final fun good3() {}
    inline final fun good4() {}


    <!DECLARATION_CANT_BE_INLINED!>inline open protected fun wrong1()<!> {}

    <!DECLARATION_CANT_BE_INLINED!>inline open public fun wrong2()<!> {}

    <!DECLARATION_CANT_BE_INLINED!>inline open fun wrong3()<!> {}

    <!DECLARATION_CANT_BE_INLINED!>inline abstract protected fun wrong4()<!>

    <!DECLARATION_CANT_BE_INLINED!>inline abstract public fun wrong5()<!>

    <!DECLARATION_CANT_BE_INLINED!>inline abstract fun wrong6()<!>
}


interface B {

    inline private fun good1() {}

    <!DECLARATION_CANT_BE_INLINED!>inline fun wrong1()<!> {}

    <!DECLARATION_CANT_BE_INLINED!>inline open fun wrong2()<!> {}

    <!DECLARATION_CANT_BE_INLINED!>inline open public fun wrong3()<!> {}

    <!DECLARATION_CANT_BE_INLINED!>inline open fun wrong4()<!> {}

    <!DECLARATION_CANT_BE_INLINED!>inline fun wrong5()<!>

    <!DECLARATION_CANT_BE_INLINED!>inline public fun wrong6()<!>

    <!DECLARATION_CANT_BE_INLINED!>inline fun wrong7()<!>
}
