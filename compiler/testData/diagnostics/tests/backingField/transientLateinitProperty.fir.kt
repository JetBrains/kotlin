// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// ISSUE: KT-73961

class Bar {
    @kotlin.jvm.Transient
    lateinit var foo: String

    constructor(foo: String) {
        this.foo = foo
    }
}
