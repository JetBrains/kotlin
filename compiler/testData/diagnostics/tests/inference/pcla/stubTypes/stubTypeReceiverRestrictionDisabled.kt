// WITH_STDLIB
// LANGUAGE: +NoBuilderInferenceWithoutAnnotationRestriction
// SKIP_TXT

fun <R> a(lambda: List<R>.(R) -> Unit) {}

fun <T> T.extension() {}

fun use(p: Any?) {}

fun test1() {
    <!INFERRED_INTO_DECLARED_UPPER_BOUNDS!>a<!> {
        this.get(0).extension()
        use(this.get(0)::extension)
        use(it::extension)
    }
}


fun test2() {
    <!INFERRED_INTO_DECLARED_UPPER_BOUNDS!>a<!> {
        val v = this.get(0)
        v.extension()
        use(v::extension)
        use(it::extension)
    }
}

fun test3() {
    operator fun <T> T.getValue(thisRef: Any?, prop: Any?): T = this
    <!INFERRED_INTO_DECLARED_UPPER_BOUNDS!>a<!> {
        val v by this.get(0)
        v.extension()
        use(v::extension)
        use(it::extension)
    }
}

class Box<TIn>(val t: TIn)

fun test4() {
    operator fun <T> T.provideDelegate(thisRef: Any?, prop: Any?): Box<T> = Box(this)
    operator fun <T> Box<T>.getValue(thisRef: Any?, prop: Any?): T = this.t
    <!INFERRED_INTO_DECLARED_UPPER_BOUNDS!>a<!> {
        val v by this.get(0)
        v.extension()
        use(v::extension)
        use(it::extension)
    }
}

fun <R> b(lambda: R.(List<R>) -> Unit) {}

fun test5() {

    operator fun <T> T.invoke(): T = this
    <!INFERRED_INTO_DECLARED_UPPER_BOUNDS!>b<!> {
        extension()
        this().extension()
        use(::extension)
    }
}

val <T> T.genericLambda: T.((T) -> Unit) -> Unit get() = {}

fun test6() {
    <!INFERRED_INTO_DECLARED_UPPER_BOUNDS!>b<!> {
        extension()
        genericLambda { }
        genericLambda { it.extension() }
        use(::extension)
    }
}
