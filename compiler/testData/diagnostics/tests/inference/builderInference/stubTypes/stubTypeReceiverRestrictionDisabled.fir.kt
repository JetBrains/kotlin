// WITH_STDLIB
// LANGUAGE: +NoBuilderInferenceWithoutAnnotationRestriction
// SKIP_TXT

fun <R> a(lambda: List<R>.(R) -> Unit) {}

fun <T> T.extension() {}

fun use(p: Any?) {}

fun test1() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>a<!> <!CANNOT_INFER_PARAMETER_TYPE!>{
        <!BUILDER_INFERENCE_STUB_RECEIVER!>this.get(0)<!>.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>extension<!>()
        use(<!BUILDER_INFERENCE_STUB_RECEIVER!>this.get(0)<!>::extension)
        use(<!BUILDER_INFERENCE_STUB_RECEIVER!>it<!>::extension)
    }<!>
}


fun test2() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>a<!> <!CANNOT_INFER_PARAMETER_TYPE!>{
        val v = this.get(0)
        <!BUILDER_INFERENCE_STUB_RECEIVER!>v<!>.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>extension<!>()
        use(<!BUILDER_INFERENCE_STUB_RECEIVER!>v<!>::extension)
        use(<!BUILDER_INFERENCE_STUB_RECEIVER!>it<!>::extension)
    }<!>
}

fun test3() {
    operator fun <T> T.getValue(thisRef: Any?, prop: Any?): T = this
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>a<!> <!CANNOT_INFER_PARAMETER_TYPE!>{
        val v by <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>this.get(0)<!>
        <!BUILDER_INFERENCE_STUB_RECEIVER!>v<!>.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>extension<!>()
        use(<!BUILDER_INFERENCE_STUB_RECEIVER!>v<!>::extension)
        use(<!BUILDER_INFERENCE_STUB_RECEIVER!>it<!>::extension)
    }<!>
}

class Box<TIn>(val t: TIn)

fun test4() {
    operator fun <T> T.provideDelegate(thisRef: Any?, prop: Any?): Box<T> = Box(this)
    operator fun <T> Box<T>.getValue(thisRef: Any?, prop: Any?): T = this.t
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>a<!> <!CANNOT_INFER_PARAMETER_TYPE!>{
        val v by <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>this.get(0)<!>
        <!BUILDER_INFERENCE_STUB_RECEIVER!>v<!>.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>extension<!>()
        use(<!BUILDER_INFERENCE_STUB_RECEIVER!>v<!>::extension)
        use(<!BUILDER_INFERENCE_STUB_RECEIVER!>it<!>::extension)
    }<!>
}

fun <R> b(lambda: R.(List<R>) -> Unit) {}

fun test5() {

    operator fun <T> T.invoke(): T = this
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>b<!> <!CANNOT_INFER_PARAMETER_TYPE!>{
        extension()
        <!CANNOT_INFER_PARAMETER_TYPE!>this<!><!NO_VALUE_FOR_PARAMETER!>()<!>.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>extension<!>()
        use(::extension)
    }<!>
}

val <T> T.genericLambda: T.((T) -> Unit) -> Unit get() = {}

fun test6() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>b<!> <!CANNOT_INFER_PARAMETER_TYPE!>{
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>extension<!>()
        <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>genericLambda<!> { }
        <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>genericLambda<!> { it.extension() }
        use(::extension)
    }<!>
}
