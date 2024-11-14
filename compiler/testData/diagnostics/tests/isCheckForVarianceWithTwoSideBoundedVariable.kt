// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-7972
// WITH_STDLIB
// DIAGNOSTICS: -DEBUG_INFO_SMARTCAST

interface P<in K, out V>
interface Inv<T> : P<T, Collection<T>> {
    fun add(x: T)
    fun get(): T
}

interface IBase {
    fun b()
}
interface IDerived : IBase

fun foo(p: P<IDerived, Collection<IBase>>, d: IDerived) {
    // Inv<T> <: P<IDerived, Collection<IBase>>
    // P<T, Collection<T>> <: P<IDerived, Collection<IBase>>
    // IDerived <: T <: IBase

    if (p is <!CANNOT_CHECK_FOR_ERASED!>Inv<in IDerived><!>) {
        p.add(d)
    }

    if (p is <!CANNOT_CHECK_FOR_ERASED!>Inv<out IBase><!>) {
        p.get().b()
    }
}
