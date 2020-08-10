// !LANGUAGE: +SuspendConversion
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun interface SuspendRunnable {
    <!FUN_INTERFACE_WITH_SUSPEND_FUNCTION!>suspend<!> fun invoke()
}

fun foo(s: SuspendRunnable) {}

fun test(f: () -> Unit) {
    foo { }
    foo(f)
}
