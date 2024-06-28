// FIR_IDENTICAL
// ISSUE: KT-57968

class Some(val child: Some?)

val Some.foo get(): Int =
    if ((child?.foo ?: 0) > 1) {
        0
    } else {
        1
    }
