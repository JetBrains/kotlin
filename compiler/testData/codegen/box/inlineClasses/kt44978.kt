// IGNORE_BACKEND: JVM
// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class StringArray(val values: Array<String>)

fun foo(a1: StringArray, a2: StringArray): String {
    var result = ""
    for ((_, a) in arrayOf(a1, a2).withIndex()) {
        result += a.values[0]
    }
    return result
}

fun box(): String = foo(StringArray(arrayOf("O")), StringArray(arrayOf("K")))
