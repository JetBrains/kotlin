// !DIAGNOSTICS: -UNUSED_PARAMETER
class A {
    constructor(x: Int)
}

val x = <!NO_VALUE_FOR_PARAMETER!>A()<!>
