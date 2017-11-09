// !DIAGNOSTICS: -UNUSED_PARAMETER

fun useSuspendFn(sfn: suspend () -> Unit) = sfn
fun useFn(fn: () -> Unit) = fn

fun ambiguous(sfn: suspend () -> Unit) = sfn
fun ambiguous(fn: () -> Unit) = fn

fun test1(sfn: suspend () -> Unit) = useFn(<!TYPE_MISMATCH!>sfn<!>)
fun test2(fn: () -> Unit) = useSuspendFn(<!TYPE_MISMATCH!>fn<!>)

fun test3(sfn: suspend () -> Unit) = useSuspendFn(sfn)
fun test4(): suspend () -> Unit = useSuspendFn {}
fun test5() = useSuspendFn {}

fun test5(sfn: suspend () -> Unit) = ambiguous(sfn)
fun test6(fn: () -> Unit) = ambiguous(fn)
fun test7(): () -> Unit = <!OVERLOAD_RESOLUTION_AMBIGUITY!>ambiguous<!> {}