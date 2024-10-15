// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class Foo {
    lateinit var x: String

    constructor(y: String) {
        x = y
    }

    constructor()
}