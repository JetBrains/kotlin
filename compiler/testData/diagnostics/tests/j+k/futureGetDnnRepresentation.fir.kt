// FULL_JDK
// LANGUAGE: +JavaTypeParameterDefaultRepresentationWithDNN
// JVM_TARGET: 1.8

import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicReference

fun foo(future: Future<String?>) {
    future.get().length
}

fun bar(threadLocal: ThreadLocal<String?>) {
    threadLocal.get().length
}

fun baz(ref: AtomicReference<String?>) {
    ref.get().length
}