// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class IC(val value: Any)

fun <T> foo(a: Result<T>, ic: IC): Pair<T, Any> = bar(a, ic, object : IFace<Result<T>, IC, Pair<T, Any>> {
    override fun call(a: Result<T>, ic: IC): Pair<T, Any> = a.getOrThrow() to ic.value
})

interface IFace<T1, T2, R> {
    fun call(t1: T1, t2: T2): R
}

fun <T1, T2, R> bar(t1: T1, t2: T2, f: IFace<T1, T2, R>): R {
    return f.call(t1, t2)
}

fun Pair<Any, Any>.join(): String = "$first$second"

fun box(): String = foo<Any>(Result.success("O"), IC("K")).join()