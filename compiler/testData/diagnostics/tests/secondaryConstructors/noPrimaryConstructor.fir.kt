// !DIAGNOSTICS: -UNUSED_PARAMETER
class A {
    constructor(x: Int)
}

val x = <!INAPPLICABLE_CANDIDATE!>A<!>()
