// WITH_RUNTIME

fun <T> foo(a: IC): T = a.value as T

@JvmInline
value class IC(val value: String)

fun box(): String {
    return foo<String>(IC("O")) + "K"
}
