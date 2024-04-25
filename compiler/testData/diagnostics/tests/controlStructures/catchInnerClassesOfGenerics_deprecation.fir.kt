// LANGUAGE: -ProhibitInnerClassesOfGenericClassExtendingThrowable
// DIAGNOSTICS: -UNUSED_VARIABLE
// JAVAC_EXPECTED_FILE

class OuterGeneric<T> {
    inner <!INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS!>class ErrorInnerExn<!> : Exception()

    inner class InnerA {
        inner <!INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS!>class ErrorInnerExn2<!> : Exception()
    }

    class OkNestedExn : Exception()

    val errorAnonymousObjectExn = <!INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS!>object<!> : Exception() {}

    fun foo() {
        <!INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS!>class OkLocalExn<!> : Exception()

        val errorAnonymousObjectExn = <!INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS!>object<!> : Exception() {}
    }

    fun <X> genericFoo() {
        <!INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS!>class OkLocalExn<!> : Exception()

        class LocalGeneric<Y> {
            inner <!INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS!>class ErrorInnerExnOfLocalGeneric<!> : Exception()
        }
    }
}

class Outer {
    inner class InnerGeneric<T> {
        inner <!INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS!>class ErrorInnerExn<!> : Exception()
    }
}

fun <T> genericFoo() {
    <!INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS!>class ErrorLocalExnInGenericFun<!> : Exception()

    val errorkAnonymousObjectExnInGenericFun = <!INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS!>object<!> : Exception() {}
}
