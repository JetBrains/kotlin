// FIR_IDENTICAL
// !DIAGNOSTICS: -UNREACHABLE_CODE -UNUSED_PARAMETER
interface Tr<T> {
    var v: T
}

class C {
    operator fun plusAssign(c: C) {}
}

fun test(t: Tr<out C>) {
    // No error because no real assignment happens
    t.v += null!!
}