// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

class Result<T> {
    fun <R> map(transform: (T) -> R): Result<R> = TODO()
}

class Tuple12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>(
    val _1: T1, val _2: T2, val _3: T3, val _4: T4, val _5: T5, val _6: T6,
    val _7: T7, val _8: T8, val _9: T9, val _10: T10, val _11: T11, val _12: T12, val _13: T13
)

fun <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> rules12(res: Result<Any>):
        Result<Tuple12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>> {
    return res.map {
        @Suppress("UNCHECKED_CAST")
        Tuple12(
            it as T1, it as T2, it as T3, it as T4, it as T5, it as T6,
            it as T7, it as T8, it as T9, it as T10, it as T11, it as T12, it as T13
        )
    }
}

