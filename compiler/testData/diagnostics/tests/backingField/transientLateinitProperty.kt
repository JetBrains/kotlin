// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
// ISSUE: KT-73961

class Bar {
    @kotlin.jvm.Transient
    <!UNNECESSARY_LATEINIT!>lateinit<!> var foo: String

    constructor(foo: String) {
        this.foo = foo
    }
}
