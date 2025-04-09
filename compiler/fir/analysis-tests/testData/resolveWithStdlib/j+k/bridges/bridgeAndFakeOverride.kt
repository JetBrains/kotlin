// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -ForbidOverloadClashesByErasure
// ISSUE: KT-13712

interface Restrict

object EmptyRestrict : Restrict

interface RestrictedGeneric<T: Restrict>: Restrict {
    fun accept(obj: T): Int
    fun acceptOpen(obj: T)
}

open class Foo {
    fun accept(obj: Restrict): Int = 0
    open fun acceptOpen(obj: Restrict) {
    }
}

class Bar : Foo(), RestrictedGeneric<Bar>{
    override fun <!ACCIDENTAL_OVERLOAD_CLASH_BY_JVM_ERASURE_WARNING!>accept<!>(obj: Bar): Int = 0
    override fun <!ACCIDENTAL_OVERLOAD_CLASH_BY_JVM_ERASURE_WARNING!>acceptOpen<!>(obj: Bar) {
    }
}
