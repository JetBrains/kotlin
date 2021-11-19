// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// FIR status: RETURN_TYPE_MISMATCH on toOptional: expected Optional<T>, actual @EN Optional<@EN T & Any>
// IGNORE_BACKEND: ANDROID
// JVM_TARGET: 1.8
// FULL_JDK

import java.util.Optional

fun <T> T?.toOptional(): Optional<T> = Optional.ofNullable(this)

fun box(): String {
    return "OK".toOptional().get()
}