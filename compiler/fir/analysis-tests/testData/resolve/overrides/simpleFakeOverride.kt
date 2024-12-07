// RUN_PIPELINE_TILL: BACKEND

open class A<T> {
    fun foo(t: T): T {
        return t
    }
}

class Some

class B : A<Some>() {
    fun test() {
        foo(Some())
    }
}

