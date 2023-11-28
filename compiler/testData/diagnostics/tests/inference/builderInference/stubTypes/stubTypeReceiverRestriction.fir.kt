// WITH_STDLIB
// SKIP_TXT

fun <R> a(lambda: List<R>.(R) -> Unit) {}

fun <T> T.extension() {}

fun use(p: Any?) {}

fun test1() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>a<!> {
        <!BUILDER_INFERENCE_STUB_RECEIVER!>this.get(0).extension()<!>
        use(<!BUILDER_INFERENCE_STUB_RECEIVER!>this.get(0)::extension<!>)
        use(<!BUILDER_INFERENCE_STUB_RECEIVER!>it::extension<!>)
    }
}


fun test2() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>a<!> {
        val v = this.get(0)
        <!BUILDER_INFERENCE_STUB_RECEIVER!>v.extension()<!>
        use(<!BUILDER_INFERENCE_STUB_RECEIVER!>v::extension<!>)
        use(<!BUILDER_INFERENCE_STUB_RECEIVER!>it::extension<!>)
    }
}

fun test3() {
    operator fun <T> T.getValue(thisRef: Any?, prop: Any?): T = this
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>a<!> {
        val v by this.get(0)
        <!BUILDER_INFERENCE_STUB_RECEIVER!>v.extension()<!>
        use(<!BUILDER_INFERENCE_STUB_RECEIVER!>v::extension<!>)
        use(<!BUILDER_INFERENCE_STUB_RECEIVER!>it::extension<!>)
    }
}

class Box<TIn>(val t: TIn)

fun test4() {
    operator fun <T> T.provideDelegate(thisRef: Any?, prop: Any?): Box<T> = Box(this)
    operator fun <T> Box<T>.getValue(thisRef: Any?, prop: Any?): T = this.t
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>a<!> {
        val v by this.get(0)
        v.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>extension<!>()
        use(v::extension)
        use(<!BUILDER_INFERENCE_STUB_RECEIVER!>it::extension<!>)
    }
}

fun <R> b(lambda: R.(List<R>) -> Unit) {}

fun test5() {

    operator fun <T> T.invoke(): T = this
    b {
        <!BUILDER_INFERENCE_STUB_RECEIVER!>extension()<!>
        <!BUILDER_INFERENCE_STUB_RECEIVER!><!BUILDER_INFERENCE_STUB_RECEIVER!>this()<!>.extension()<!>
        <!BUILDER_INFERENCE_STUB_RECEIVER!>use(<!BUILDER_INFERENCE_STUB_RECEIVER!>::extension<!>)<!>
    }
}

val <T> T.genericLambda: T.((T) -> Unit) -> Unit get() = {}

fun test6() {
    b {
        <!BUILDER_INFERENCE_STUB_RECEIVER!>extension()<!>
        <!BUILDER_INFERENCE_STUB_RECEIVER!>genericLambda { }<!>
        <!BUILDER_INFERENCE_STUB_RECEIVER!>genericLambda { it.extension() }<!>
        <!BUILDER_INFERENCE_STUB_RECEIVER!>use(<!BUILDER_INFERENCE_STUB_RECEIVER!>::extension<!>)<!>
    }
}
