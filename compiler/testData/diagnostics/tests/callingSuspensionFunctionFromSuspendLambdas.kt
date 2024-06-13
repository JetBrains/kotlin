// FIR_IDENTICAL
// ISSUE: KT-68351
// FULL_JDK

import java.lang.ref.WeakReference

suspend fun test() {}

fun foo() {
    WeakReference<suspend () -> Unit> {
        test()
    }
}
