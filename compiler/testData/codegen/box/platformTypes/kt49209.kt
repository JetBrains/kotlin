// TARGET_BACKEND: JVM
// FULL_JDK

import java.util.Optional

fun <T> T?.toOptional(): Optional<T> = Optional.ofNullable(this)

fun box(): String {
    return "OK".toOptional().get()
}