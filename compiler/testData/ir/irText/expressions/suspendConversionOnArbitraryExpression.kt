// LANGUAGE: +SuspendConversion

fun useSuspend(sfn: suspend () -> Unit) {}

fun useSuspendExt(sfn: suspend Int.() -> Unit) {}

fun useSuspendArg(sfn: suspend (Int) -> Unit) {}

fun <T> useSuspendArgT(sfn: suspend (T) -> Unit) {}

fun <T> useSuspendExtT(sfn: suspend T.() -> Unit) {}

fun produceFun(): () -> Unit = {}

fun testSimple(fn: () -> Unit) {
    useSuspend(fn)
}

fun testSimpleNonVal() {
    useSuspend(produceFun())
}

fun testExtAsExt(fn: Int.() -> Unit) {
    useSuspendExt(fn)
}

fun testExtAsSimple(fn: Int.() -> Unit) {
    useSuspendArg(fn)
}

fun testSimpleAsExt(fn: (Int) -> Unit) {
    useSuspendExt(fn)
}

fun testSimpleAsSimpleT(fn: (Int) -> Unit) {
    useSuspendArgT(fn)
}

fun testSimpleAsExtT(fn: (Int) -> Unit) {
    useSuspendExtT(fn)
}

fun testExtAsSimpleT(fn: Int.() -> Unit) {
    useSuspendArgT(fn)
}

fun testExtAsExtT(fn: Int.() -> Unit) {
    useSuspendExtT(fn)
}

fun <S> testSimpleSAsSimpleT(fn: (S) -> Unit) {
    useSuspendArgT(fn)
}

fun <S> testSimpleSAsExtT(fn: (S) -> Unit) {
    useSuspendExtT(fn)
}

fun <S> testExtSAsSimpleT(fn: S.() -> Unit) {
    useSuspendArgT(fn)
}

fun <S> testExtSAsExtT(fn: S.() -> Unit) {
    useSuspendExtT(fn)
}

fun testSmartCastWithSuspendConversion(a: Any) {
    a as () -> Unit
    useSuspend(a)
}

fun testSmartCastOnVarWithSuspendConversion(a: Any) {
    var b = a
    b as () -> Unit
    useSuspend(b)
}

fun testSmartCastVsSuspendConversion(a: () -> Unit) {
    a as suspend () -> Unit
    useSuspend(a)
}

fun testSmartCastOnVarVsSuspendConversion(a: () -> Unit) {
    var b = a
    b as suspend () -> Unit
    useSuspend(b)
}

fun <T> testIntersectionVsSuspendConversion(x: T)
        where T : () -> Unit, T : suspend () -> Unit {
    useSuspend(x)
}
