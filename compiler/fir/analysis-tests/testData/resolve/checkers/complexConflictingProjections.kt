// ISSUE: KT-49024

class Foo
class Bar<T1: <!FINAL_UPPER_BOUND!>Foo<!>, out T2>
class Baz<T1, T2: Bar<Foo, out T2>>
class Qux<T1, T2: Baz<T2, <!UPPER_BOUND_VIOLATED!>Bar<Foo, T2><!>>>(var f: T2)

class Quux<T> {
    fun test(): Unit {
        val x: Qux<in T, <!UPPER_BOUND_VIOLATED!>Baz<T, <!UPPER_BOUND_VIOLATED!>Bar<Foo, <!CONFLICTING_PROJECTION!>in<!> T><!>><!>> = null!!
        x.f = null!!
    }
}
