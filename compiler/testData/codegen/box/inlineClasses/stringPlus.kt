// WITH_STDLIB

fun <T> foo(a: IC): T = a.value as T

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class IC(val value: String)

fun box(): String {
    return foo<String>(IC("O")) + "K"
}
