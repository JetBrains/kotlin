// WITH_STDLIB
// SKIP_TXT

fun <R> a(lambda: List<R>.(R) -> Unit) {}

fun <T> T.extension() {}

fun use(p: Any?) {}

fun test1() {
    <!INFERRED_INTO_DECLARED_UPPER_BOUNDS!>a<!> {
        <!BUILDER_INFERENCE_STUB_RECEIVER("R; a")!>this.get(0)<!>.extension()
        use(<!BUILDER_INFERENCE_STUB_RECEIVER("R; a")!>this.get(0)<!>::extension)
        use(<!BUILDER_INFERENCE_STUB_RECEIVER("R; a")!>it<!>::extension)
    }
}


fun test2() {
    <!INFERRED_INTO_DECLARED_UPPER_BOUNDS!>a<!> {
        val v = this.get(0)
        <!BUILDER_INFERENCE_STUB_RECEIVER("R; a")!>v<!>.extension()
        use(<!BUILDER_INFERENCE_STUB_RECEIVER("R; a")!>v<!>::extension)
        use(<!BUILDER_INFERENCE_STUB_RECEIVER("R; a")!>it<!>::extension)
    }
}

fun test3() {
    operator fun <T> T.getValue(thisRef: Any?, prop: Any?): T = this
    <!INFERRED_INTO_DECLARED_UPPER_BOUNDS!>a<!> {
        val v by <!BUILDER_INFERENCE_STUB_RECEIVER("R; a")!>this.get(0)<!>
        <!BUILDER_INFERENCE_STUB_RECEIVER("R; a")!>v<!>.extension()
        use(<!BUILDER_INFERENCE_STUB_RECEIVER("R; a")!>v<!>::extension)
        use(<!BUILDER_INFERENCE_STUB_RECEIVER("R; a")!>it<!>::extension)
    }
}

class Box<TIn>(val t: TIn)

fun test4() {
    operator fun <T> T.provideDelegate(thisRef: Any?, prop: Any?): Box<T> = Box(this)
    operator fun <T> Box<T>.getValue(thisRef: Any?, prop: Any?): T = this.t
    <!INFERRED_INTO_DECLARED_UPPER_BOUNDS!>a<!> {
        val v by <!BUILDER_INFERENCE_STUB_RECEIVER("R; a")!>this.get(0)<!>
        <!BUILDER_INFERENCE_STUB_RECEIVER("R; a")!>v<!>.extension()
        use(<!BUILDER_INFERENCE_STUB_RECEIVER("R; a")!>v<!>::extension)
        use(<!BUILDER_INFERENCE_STUB_RECEIVER("R; a")!>it<!>::extension)
    }
}

fun <R> b(lambda: R.(List<R>) -> Unit) {}

fun test5() {

    operator fun <T> T.invoke(): T = this
    <!INFERRED_INTO_DECLARED_UPPER_BOUNDS!>b<!> {
        <!BUILDER_INFERENCE_STUB_RECEIVER("R; b")!>extension()<!>
        <!BUILDER_INFERENCE_STUB_RECEIVER("R; b")!><!BUILDER_INFERENCE_STUB_RECEIVER("R; b")!>this<!>()<!>.extension()
        <!BUILDER_INFERENCE_STUB_RECEIVER("R; b")!>use(::extension)<!>
    }
}

val <T> T.genericLambda: T.((T) -> Unit) -> Unit get() = {}

fun test6() {
    <!INFERRED_INTO_DECLARED_UPPER_BOUNDS!>b<!> {
        <!BUILDER_INFERENCE_STUB_RECEIVER("R; b")!>extension()<!>
        <!BUILDER_INFERENCE_STUB_RECEIVER("R; b")!>genericLambda<!> { }
        <!BUILDER_INFERENCE_STUB_RECEIVER("R; b")!>genericLambda<!> { <!BUILDER_INFERENCE_STUB_RECEIVER("R; b")!>it<!>.extension() }
        <!BUILDER_INFERENCE_STUB_RECEIVER("R; b")!>use(::extension)<!>
    }
}
