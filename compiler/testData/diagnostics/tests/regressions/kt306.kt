// FIR_IDENTICAL
// KT-306 Ambiguity when different this's have same-looking functions

fun test() {
    (fun Foo.() {
        bar()
        (fun Barr.() {
            this.bar()
            bar()
        })
    })
    (fun Barr.() {
        this.bar()
        bar()
    })
}

class Foo {
    fun bar() {}
}

class Barr {
    fun bar() {}
}
