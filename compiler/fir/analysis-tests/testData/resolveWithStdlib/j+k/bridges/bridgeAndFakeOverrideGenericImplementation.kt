// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -ForbidOverloadClashesByErasure
// ISSUE: KT-13712

interface Restrict

object EmptyRestrict : Restrict

interface RestrictedGeneric<T  : Restrict>: Restrict {
    fun accept(obj: T): Int
}

open class Foo {
    fun accept(obj: Restrict): Int = 0
}

class Bar<T> : Foo(), RestrictedGeneric<Bar<T>>{
    override fun <!ACCIDENTAL_OVERLOAD_CLASH_BY_JVM_ERASURE_WARNING!>accept<!>(obj: Bar<T>): Int = 0
}
