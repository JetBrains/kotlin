// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -ForbidOverloadClashesByErasure
// ISSUE: KT-13712

interface Restrict

object EmptyRestrict : Restrict

interface RestrictedGeneric<T : Restrict>: Restrict {
    fun accept(obj: T): Int
}

open class Foo<D : Restrict> {
    fun accept(obj: D): Int = 0
}

class Bar : Foo<EmptyRestrict>(), RestrictedGeneric<Bar> {
    override fun accept(obj: Bar): Int = 0
}
