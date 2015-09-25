// !DIAGNOSTICS: -UNREACHABLE_CODE
interface Tr<T> {
    var v: T
}

class C {
    operator fun plusAssign(<!UNUSED_PARAMETER!>c<!>: C) {}
}

fun test(t: Tr<out C>) {
    // No error because no real assignment happens
    t.v += null!!
}