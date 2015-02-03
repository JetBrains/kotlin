// !DIAGNOSTICS: -UNUSED_PARAMETER
class A {
    constructor(x: Int) {}
    constructor(x: String) {}
    constructor(): <!NONE_APPLICABLE!>this<!>('a') {}
}
