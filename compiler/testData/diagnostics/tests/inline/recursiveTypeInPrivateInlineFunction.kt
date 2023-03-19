// FIR_IDENTICAL
// ISSUE: KT-56692

private <!NOTHING_TO_INLINE!>inline<!> fun check(inf: Self<*>) = inf
class Self<T : Self<T>>
