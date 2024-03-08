// FIR_IDENTICAL
// FULL_JDK
// JVM_TARGET: 1.8

import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicReference

fun foo(future: Future<String?>) {
    future.get()<!UNSAFE_CALL!>.<!>length
}

fun bar(threadLocal: ThreadLocal<String?>) {
    threadLocal.get()<!UNSAFE_CALL!>.<!>length
}

fun baz(ref: AtomicReference<String?>) {
    ref.get()<!UNSAFE_CALL!>.<!>length
}
