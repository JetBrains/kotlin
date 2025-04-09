// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -ForbidOverloadClashesByErasure
// ISSUE: KT-13712

open class A {
    fun f(x: Any) {
    }
}

interface B<T> {
    fun f(x: T) {
    }
}

class C : B<String>, A()
