// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class IC(val value: Any)

fun <T> foo(a: Result<T>, ic: IC): Pair<T, Any> = bar(a, ic) { a, ic ->
    a.getOrThrow() to ic.value
}

fun <T1, T2, R> bar(t1: T1, t2: T2, f: (T1, T2) -> R): R {
    return f(t1, t2)
}

fun Pair<Any, Any>.join(): String = "$first$second"

fun box(): String = foo<Any>(Result.success("O"), IC("K")).join()