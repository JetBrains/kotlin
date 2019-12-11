// !LANGUAGE: +ProhibitInnerClassesOfGenericClassExtendingThrowable
// !DIAGNOSTICS: -UNUSED_VARIABLE
// JAVAC_EXPECTED_FILE

class OuterGeneric<T> {
    inner class ErrorInnerExn : Exception()

    inner class InnerA {
        inner class ErrorInnerExn2 : Exception()
    }

    class OkNestedExn : Exception()

    val errorAnonymousObjectExn = object : Exception() {}

    fun foo() {
        class OkLocalExn : Exception()

        val errorAnonymousObjectExn = object : Exception() {}
    }

    fun <X> genericFoo() {
        class OkLocalExn : Exception()

        class LocalGeneric<Y> {
            inner class ErrorInnerExnOfLocalGeneric : Exception()
        }
    }
}

class Outer {
    inner class InnerGeneric<T> {
        inner class ErrorInnerExn : Exception()
    }
}

fun <T> genericFoo() {
    class ErrorLocalExnInGenericFun : Exception()

    val errorkAnonymousObjectExnInGenericFun = object : Exception() {}
}
