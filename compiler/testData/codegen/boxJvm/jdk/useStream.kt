// SKIP_JDK6
// TARGET_BACKEND: JVM
// KT-5190 Java 8 Stream.collect couldn't be called
// WITH_STDLIB
// FULL_JDK

import java.util.stream.Collectors
import java.util.stream.Stream

fun box(): String {
    val mutableListOf = mutableListOf("OK", "B", "C")

    return test((mutableListOf as java.util.Collection<String>).stream()) as String
}

fun test(a: Stream<String>) = a.collect(Collectors.toList()).first()
