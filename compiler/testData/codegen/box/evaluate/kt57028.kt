// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK

// java.lang.NoSuchMethodError: java.lang.String.chars
// IGNORE_BACKEND: ANDROID
import kotlin.streams.toList

fun box(): String {
    val shoulNotBeEveluated1 = "HelloWorld".chars()
    val shoulNotBeEveluated2 = "HelloWorld".codePoints()
    val shoulNotBeEveluated3 = "HelloWorld".chars().toList().groupBy { it }.map { it.key to it.value.size }.joinToString().also(::println)
    return "OK"
}
