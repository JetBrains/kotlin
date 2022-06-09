// TARGET_BACKEND: JVM
// IGNORE_BACKEND: ANDROID
// JVM_TARGET: 1.8
// FULL_JDK

import java.util.Optional

fun <T> T?.toOptional(): Optional<T & Any> = Optional.ofNullable(this)

fun box(): String {
    return "OK".toOptional().get()
}