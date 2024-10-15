// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// FIR_DUMP
// SKIP_TXT

fun Any?.test() {}

class Bar {
    fun test() {}
}

fun main() {

    buildList {
        add(Bar())
        <!BUILDER_INFERENCE_STUB_RECEIVER!>this.get(0)<!>.test() // resolved to Any?.test in K1 and to Bar.test in K2
    }
    buildList<Bar> {
        add(Bar())
        this.get(0).test() // resolved to Bar.test
    }
}
